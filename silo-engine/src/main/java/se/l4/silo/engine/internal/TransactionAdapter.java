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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.commons.io.Bytes;
import se.l4.commons.io.ExtendedDataInput;
import se.l4.commons.io.IoConsumer;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.tx.TransactionOperation;
import se.l4.silo.engine.internal.tx.TransactionOperationType;
import se.l4.silo.engine.log.LogEntry;
import se.l4.silo.engine.types.LongArrayFieldType;
import se.l4.vibe.Vibe;
import se.l4.vibe.percentile.CombinedProbes;
import se.l4.vibe.probes.CountingProbe;

/**
 * Adapter that handles the translation from individual transaction events
 * to events that are applied to the storage.
 * 
 * @author Andreas Holstenson
 *
 */
public class TransactionAdapter
	implements IoConsumer<LogEntry>
{
	// Timeout to use for removing stale transactions
	private static final long TIMEOUT = TimeUnit.HOURS.toMillis(24);
	
	private static final Logger logger = LoggerFactory.getLogger(TransactionAdapter.class);
	
	private final StorageApplier applier;
	private final MVStoreManager store;
	
	private volatile MVMap<long[], TransactionOperation> log;

	private final CountingProbe activeTx;
	private final CountingProbe logEvents;

	private final CountingProbe txStarts;
	private final CountingProbe txCommits;
	private final CountingProbe txRollbacks;

	public TransactionAdapter(Vibe vibe, ScheduledExecutorService executor, MVStoreManager store, StorageApplier applier)
	{
		this.store = store;
		this.applier = applier;
		
		activeTx = new CountingProbe(false);
		
		txStarts = new CountingProbe();
		txCommits = new CountingProbe();
		txRollbacks = new CountingProbe();
		
		logEvents = new CountingProbe();
		
		if(vibe != null)
		{
			vibe.sample(CombinedProbes.<Long>builder()
					.add("active", activeTx)
					.add("starts", txStarts)
					.add("commits", txCommits)
					.add("rollbacks", txRollbacks)
					.create()
				)
				.at("tx/summary")
				.export();
			
			vibe.sample(logEvents)
				.at("log/events")
				.export();
		}
		
		reopen();
		
		if(executor != null)
		{
			executor.scheduleAtFixedRate(this::removeStale, 1, 5, TimeUnit.MINUTES);
		}
	}
	
	public void reopen()
	{
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
		
		logger.info(activeTx.peek() + " active transactions spread over " + log.size() + " entries in the log");
	}
	
	@Override
	public void accept(LogEntry item)
		throws IOException
	{
		logEvents.increase();
		
		try(ExtendedDataInput in = item.getData().asDataInput())
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
	
	@VisibleForTesting
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
				
				logger.info("Reduced to " + activeTx.peek() + " active transactions");
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
		List<long[]> keys = Lists.newArrayList();
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
						ChunkedBytes bytes = new ChunkedBytes(keys);
						applier.store(op.getEntity(), op.getId(), bytes);
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
	
	private class ChunkedBytes
		implements Bytes
	{
		private final List<long[]> keys;

		public ChunkedBytes(List<long[]> keys)
		{
			this.keys = keys;
		}
		
		@Override
		public InputStream asInputStream() throws IOException
		{
			return new SequenceInputStream(new InputStreamEnumeration(keys));
		}
		
		@Override
		public byte[] toByteArray() throws IOException
		{
			return ByteStreams.toByteArray(asInputStream());
		}
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
