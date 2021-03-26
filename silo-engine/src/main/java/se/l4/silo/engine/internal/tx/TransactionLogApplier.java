package se.l4.silo.engine.internal.tx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.scheduler.Scheduler;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.MessageConstants;
import se.l4.silo.engine.internal.StorageApplier;
import se.l4.silo.engine.internal.tx.operations.ChunkOperation;
import se.l4.silo.engine.internal.tx.operations.DeleteOperation;
import se.l4.silo.engine.internal.tx.operations.IndexChunkOperation;
import se.l4.silo.engine.internal.tx.operations.StartOperation;
import se.l4.silo.engine.internal.tx.operations.StoreChunkOperation;
import se.l4.silo.engine.internal.tx.operations.TransactionOperation;
import se.l4.silo.engine.internal.types.LongArrayFieldType;
import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.log.LogEntry;
import se.l4.vibe.Vibe;
import se.l4.vibe.operations.Change;
import se.l4.vibe.probes.CountingProbe;
import se.l4.vibe.probes.SampledProbe;
import se.l4.vibe.snapshots.MapSnapshot;
import se.l4.ylem.io.IOConsumer;

/**
 * Adapter that handles the translation from individual transaction events
 * to events that are applied to the storage.
 */
public class TransactionLogApplier
	implements IOConsumer<LogEntry>
{
	// Timeout to use for removing stale transactions
	private static final long TIMEOUT = TimeUnit.HOURS.toMillis(24);

	private static final Logger logger = LoggerFactory.getLogger(TransactionLogApplier.class);

	private final StorageApplier applier;

	private final MVMap<long[], TransactionOperation> log;

	private final CountingProbe activeTx;
	private final CountingProbe logEvents;

	private final CountingProbe txStarts;
	private final CountingProbe txCommits;
	private final CountingProbe txRollbacks;

	public TransactionLogApplier(
		Vibe vibe,
		Scheduler scheduler,
		MVStoreManager store,
		StorageApplier applier
	)
	{
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

		log = store.openMap("tx.log", new MVMap.Builder<long[], TransactionOperation>()
			.keyType(LongArrayFieldType.INSTANCE)
			.valueType(new TransactionOperationType())
		);

		if(! log.isEmpty())
		{
			Iterator<long[]> it = log.keyIterator(log.firstKey());
			while(it.hasNext())
			{
				long[] key = it.next();
				TransactionOperation op = log.get(key);
				if(op instanceof StartOperation)
				{
					activeTx.increase();
				}
			}
		}

		logger.debug(activeTx.read() + " active transactions spread over " + log.size() + " entries in the log");

		if(scheduler != null)
		{
			scheduler.schedulePeriodically(this::removeStale, 1, 5, TimeUnit.MINUTES);
		}
	}

	@Override
	public void accept(LogEntry item)
		throws IOException
	{
		logEvents.increase();

		try(InputStream stream = item.getData().asInputStream())
		{
			BinaryDataInput in = BinaryDataInput.forStream(stream);
			int msgType = in.read();

			long tx = in.readVLong();
			long[] key = new long[] { tx, findNextId(tx) };

			switch(msgType)
			{
				case MessageConstants.START_TRANSACTION:
					// TODO: This should start an automatic transaction rollback timer
					log.put(key, StartOperation.read(in));

					activeTx.increase();
					txStarts.increase();

					if(logger.isTraceEnabled())
					{
						logger.trace("[" + tx + "] Starting transaction");
					}

					break;
				case MessageConstants.STORE_CHUNK:
					log.put(key, StoreChunkOperation.read(in));
					break;
				case MessageConstants.INDEX_CHUNK:
					log.put(key, IndexChunkOperation.read(in));
					break;
				case MessageConstants.DELETE:
					log.put(key, DeleteOperation.read(in));
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
			MutableLongSet toRemove = new LongHashSet();
			Iterator<long[]> it = log.keyIterator(log.firstKey());
			while(it.hasNext())
			{
				long[] key = it.next();
				TransactionOperation op = log.get(key);
				if(op instanceof StartOperation)
				{
					long time = System.currentTimeMillis();
					if(time - ((StartOperation) op).getTimestamp() >= TIMEOUT)
					{
						toRemove.add(key[0]);
					}
				}
			}

			if(! toRemove.isEmpty())
			{
				logger.info("Removing " + toRemove.size() + " stale transactions");

				toRemove.each(this::removeTransaction);

				logger.info("Reduced to " + activeTx.read() + " active transactions");
			}
		}
	}

	private long findNextId(long tx)
	{
		long[] ceil = log.floorKey(new long[] { tx, Integer.MAX_VALUE });
		if(ceil == null || ceil[0] != tx)
		{
			return 0;
		}
		else
		{
			return ceil[1] + 1;
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
		applier.transactionStart(tx);

		Iterator<long[]> it = log.keyIterator(new long[] { tx, 0l });
		List<long[]> keys = new ArrayList<>();
		while(it.hasNext())
		{
			long[] key = it.next();
			if(key[0] != tx) break;

			TransactionOperation op = log.get(key);
			if(op instanceof DeleteOperation)
			{
				DeleteOperation delete = (DeleteOperation) op;
				applier.delete(delete.getCollection(), delete.getId());
			}
			else if(op instanceof StoreChunkOperation)
			{
				StoreChunkOperation store = (StoreChunkOperation) op;
				if(store.getData().length == 0)
				{
					// Zero length chunk means end of data
					applier.store(
						store.getCollection(),
						store.getId(),
						new SequenceInputStream(new InputStreamEnumeration(keys))
					);

					keys.clear();
				}
				else
				{
					keys.add(key);
				}
			}
			else if(op instanceof IndexChunkOperation)
			{
				IndexChunkOperation indexChunk = (IndexChunkOperation) op;
				if(indexChunk.getData().length == 0)
				{
					// Zero length chunk means end of data
					applier.index(
						indexChunk.getCollection(),
						indexChunk.getIndex(),
						indexChunk.getId(),
						new SequenceInputStream(new InputStreamEnumeration(keys))
					);

					keys.clear();
				}
				else
				{
					keys.add(key);
				}
			}
		}

		removeTransaction(tx);

		// Indicate that the TX has been applied
		applier.transactionComplete(tx, null);
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
			byte[] data = ((ChunkOperation) log.get(key)).getData();

			if(logger.isTraceEnabled())
			{
				logger.trace("[" + key[0] + "] Reading id " + key[1] + " with data " + Base64.getEncoder().encodeToString(data));
			}

			return new ByteArrayInputStream(data);
		}
	}
}
