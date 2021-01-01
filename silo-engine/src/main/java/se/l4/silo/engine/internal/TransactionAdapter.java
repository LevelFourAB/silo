package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.cursors.LongCursor;

import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.tx.TransactionOperation;
import se.l4.silo.engine.internal.tx.TransactionOperationType;
import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.log.LogEntry;
import se.l4.silo.engine.types.LongArrayFieldType;
import se.l4.vibe.Vibe;
import se.l4.vibe.operations.Change;
import se.l4.vibe.probes.CountingProbe;
import se.l4.vibe.probes.SampledProbe;
import se.l4.vibe.snapshots.MapSnapshot;
import se.l4.ylem.io.IOConsumer;

/**
 * Adapter that handles the translation from individual transaction events
 * to events that are applied to the storage.
 *
 * @author Andreas Holstenson
 *
 */
public class TransactionAdapter
	implements IOConsumer<LogEntry>
{
	// Timeout to use for removing stale transactions
	private static final long TIMEOUT = TimeUnit.HOURS.toMillis(24);

	private static final Logger logger = LoggerFactory.getLogger(TransactionAdapter.class);

	private final StorageApplier applier;
	private final MVStoreManager store;

	private final MVMap<long[], TransactionOperation> log;

	private final CountingProbe activeTx;
	private final CountingProbe logEvents;

	private final CountingProbe txStarts;
	private final CountingProbe txCommits;
	private final CountingProbe txRollbacks;

	public TransactionAdapter(Vibe vibe, ScheduledExecutorService executor, MVStoreManager store, StorageApplier applier)
	{
		this.store = store;
		this.applier = applier;

		activeTx = new CountingProbe();

		txStarts = new CountingProbe();
		txCommits = new CountingProbe();
		txRollbacks = new CountingProbe();

		logEvents = new CountingProbe();

		if(vibe != null)
		{
			SampledProbe<MapSnapshot> summaryProbe = SampledProbe.merged()
				.add("active", SampledProbe.over(activeTx))
				.add("starts", txStarts.apply(Change.changeAsLong()))
				.add("commits", txCommits.apply(Change.changeAsLong()))
				.add("rollbacks", txRollbacks.apply(Change.changeAsLong()))
				.build();

			vibe.export(summaryProbe)
				.at("tx", "summary")
				.done();

			vibe.export(logEvents.apply(Change.changeAsLong()))
				.at("log", "events")
				.done();
		}

		log = store.openMap("tx.log", new LongArrayFieldType(), new TransactionOperationType());
		if(! log.isEmpty())
		{
			Iterator<long[]> it = log.keyIterator(log.firstKey());
			while(it.hasNext())
			{
				long[] key = it.next();
				TransactionOperation op = log.get(key);
				if(op.getType() == TransactionOperation.Type.START)
				{
					activeTx.increase();
				}
			}
		}

		logger.debug(activeTx.read() + " active transactions spread over " + log.size() + " entries in the log");

		if(executor != null)
		{
			executor.scheduleAtFixedRate(this::removeStale, 1, 5, TimeUnit.MINUTES);
		}
	}

	@Override
	public void accept(LogEntry item)
		throws IOException
	{
		logEvents.increase();

		try(ExtendedDataInput in = new ExtendedDataInputStream(item.getData().asInputStream()))
		{
			int msgType = in.readVInt();
			long tx = in.readVLong();
			switch(msgType)
			{
				case MessageConstants.START_TRANSACTION:
					// TODO: This should start an automatic transaction rollback timer
					start(tx, item.getTimestamp());
					txStarts.increase();
					break;
				case MessageConstants.STORE_CHUNK:
					// Store data
					{
						String entity = in.readString();
						Object id = IOUtils.readId(in);
						byte[] data = IOUtils.readByteArray(in);
						storeChunk(tx, entity, id, data);
					}
					break;
				case MessageConstants.DELETE:
					{
						String entity = in.readString();
						Object id = IOUtils.readId(in);
						delete(tx, entity, id);
					}
					break;
				case MessageConstants.INDEX_CHUNK:
					// Store data
					{
						String entity = in.readString();
						String index = in.readString();
						Object id = IOUtils.readId(in);
						byte[] data = IOUtils.readByteArray(in);
						indexChunk(tx, entity, index, id, data);
					}
					break;
				case MessageConstants.COMMIT_TRANSACTION:
					txCommits.increase();
					applyTransaction(tx);
					break;
				case MessageConstants.ROLLBACK_TRANSACTION:
					txRollbacks.increase();
					removeTransaction(tx);
					break;
			}
		}
	}

	public void removeStale()
	{
		logger.trace("Looking for stale transactions");
		if(! log.isEmpty())
		{
			LongSet toRemove = new LongHashSet();
			Iterator<long[]> it = log.keyIterator(log.firstKey());
			while(it.hasNext())
			{
				long[] key = it.next();
				TransactionOperation op = log.get(key);
				if(op.getType() == TransactionOperation.Type.START)
				{
					long time = System.currentTimeMillis();
					if(time - op.getTimestamp() >= TIMEOUT)
					{
						toRemove.add(key[0]);
					}
				}
			}

			if(! toRemove.isEmpty())
			{
				logger.info("Removing " + toRemove.size() + " stale transactions");

				for(LongCursor c : toRemove)
				{
					removeTransaction(c.value);
				}

				logger.info("Reduced to " + activeTx.read() + " active transactions");
			}
		}
	}

	/**
	 * Start of transaction.
	 *
	 * @param tx
	 * @param entity
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	private void start(long tx, long timestamp)
		throws IOException
	{
		// Locate the next local id for this transaction
		Iterator<long[]> it = log.keyIterator(new long[] { tx, 0l });
		long nextId = 0;
		while(it.hasNext())
		{
			long[] key = it.next();
			if(key[0] != tx) break;

			nextId = (key[1]) + 1;
		}

		// Store the data
		long[] key = new long[] { tx, nextId };
		log.put(key, TransactionOperation.start(timestamp));

		activeTx.increase();

		if(logger.isTraceEnabled())
		{
			logger.trace("[" + tx + "] Starting transaction");
		}
	}

	/**
	 * Store information about a store operation for a given entity. This
	 * method will take chunked data and store it in the internal log.
	 *
	 * @param tx
	 * @param entity
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	private void storeChunk(long tx, String entity, Object id, byte[] data)
		throws IOException
	{
		// Locate the next local id for this transaction
		long[] ceil = log.floorKey(new long[] { tx, Integer.MAX_VALUE });
		long nextId;
		if(ceil == null || ceil[0] != tx)
		{
			nextId = 0;
		}
		else
		{
			nextId = ceil[1] + 1;
		}

		// Store the data
		long[] key = new long[] { tx, nextId };
		log.put(key, TransactionOperation.store(entity, id, data));

		if(logger.isTraceEnabled())
		{
			logger.trace("[" + tx + "] Wrote " + nextId + " for " + entity + "[" + id + "] with data " + Base64.getEncoder().encodeToString(data));
		}
	}

	/**
	 * Indicate that something has been deleted in a transaction.
	 *
	 * @param tx
	 * @param entity
	 * @param id
	 */
	private void delete(long tx, String entity, Object id)
	{
		// Locate the next local id for this transaction
		Iterator<long[]> it = log.keyIterator(new long[] { tx, 0l });
		long nextId = 0;
		while(it.hasNext())
		{
			long[] key = it.next();
			if(key[0] != tx) break;

			nextId = (key[1]) + 1;
		}

		long[] key = new long[] { tx, nextId };
		log.put(key, TransactionOperation.delete(entity, id));

		if(logger.isTraceEnabled())
		{
			logger.trace("[" + tx + "] Wrote " + nextId + " as delete of " + entity + "[" + id + "]");
		}
	}

		/**
	 * Store information about a store operation for a given entity. This
	 * method will take chunked data and store it in the internal log.
	 *
	 * @param tx
	 * @param entity
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	private void indexChunk(long tx, String entity, String index, Object id, byte[] data)
		throws IOException
	{
		// Locate the next local id for this transaction
		long[] ceil = log.floorKey(new long[] { tx, Integer.MAX_VALUE });
		long nextId;
		if(ceil == null || ceil[0] != tx)
		{
			nextId = 0;
		}
		else
		{
			nextId = ceil[1] + 1;
		}

		// Store the data
		long[] key = new long[] { tx, nextId };
		log.put(key, TransactionOperation.indexChunk(entity, index, id, data));

		if(logger.isTraceEnabled())
		{
			logger.trace("[" + tx + "] Wrote " + nextId + " for " + entity + "::" + index + "[" + id + "] with data " + Base64.getEncoder().encodeToString(data));
		}
	}

	/**
	 * Remove a transaction from the log.
	 *
	 * @param tx
	 */
	private void removeTransaction(long tx)
	{
		List<long[]> keysToRemove = new ArrayList<>();
		Iterator<long[]> it = log.keyIterator(new long[] { tx, 0l });
		while(it.hasNext())
		{
			long[] key = it.next();
			if(key[0] != tx) break;

			keysToRemove.add(key);
		}

		for(Object o : keysToRemove)
		{
			log.remove(o);
		}

		activeTx.decrease();

		if(logger.isTraceEnabled())
		{
			logger.trace("[" + tx + "] Removing from stored log");
		}
	}

	/**
	 * Apply a transaction to the storage.
	 *
	 * @param tx
	 */
	private void applyTransaction(long tx)
		throws IOException
	{
		Iterator<long[]> it = log.keyIterator(new long[] { tx, 0l });
		List<long[]> keys = new ArrayList<>();
		while(it.hasNext())
		{
			long[] key = it.next();
			if(key[0] != tx) break;

			TransactionOperation op = log.get(key);
			switch(op.getType())
			{
				case DELETE:
					applier.delete(op.getEntity(), op.getId());
					break;
				case STORE_CHUNK:
					if(op.getData().length == 0)
					{
						// Zero length chunk means end of data
						applier.store(
							op.getEntity(),
							op.getId(),
							new SequenceInputStream(new InputStreamEnumeration(keys))
						);
						keys.clear();
					}
					else
					{
						keys.add(key);
					}
					break;
				case INDEX_CHUNK:
					if(op.getData().length == 0)
					{
						// Zero length chunk means end of data
						String rawEntity = op.getEntity();
						int idx = rawEntity.lastIndexOf("::");
						applier.index(
							rawEntity.substring(0, idx),
							rawEntity.substring(idx + 2),
							op.getId(),
							new SequenceInputStream(new InputStreamEnumeration(keys))
						);
						keys.clear();
					}
					else
					{
						keys.add(key);
					}
					break;
				default:
					// Do nothing for other types
			}
		}

		removeTransaction(tx);
	}

	private class InputStreamEnumeration
		implements Enumeration<InputStream>
	{
		private final Iterator<long[]> it;


		public InputStreamEnumeration(List<long[]> keys)
		{
			it = keys.iterator();
		}

		@Override
		public boolean hasMoreElements()
		{
			return it.hasNext();
		}

		@Override
		public InputStream nextElement()
		{
			long[] key = it.next();

			if(logger.isTraceEnabled())
			{
				logger.trace("[" + key[0] + "] Reading id " + key[1] + " with data " + Base64.getEncoder().encodeToString(log.get(key).getData()));
			}

			return new ByteArrayInputStream(log.get(key).getData());
		}


	}
}
