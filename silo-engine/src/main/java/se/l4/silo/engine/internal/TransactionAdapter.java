package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.h2.mvstore.MVMap;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.core.io.ExtendedDataInput;
import se.l4.aurochs.core.io.IoConsumer;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.tx.TransactionOperation;
import se.l4.silo.engine.internal.tx.TransactionOperationType;
import se.l4.silo.engine.log.LogEntry;
import se.l4.silo.engine.types.LongArrayFieldType;

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
	private final StorageApplier applier;
	private final MVStoreManager store;
	
	private volatile MVMap<long[], TransactionOperation> log;

	public TransactionAdapter(MVStoreManager store, StorageApplier applier)
	{
		this.store = store;
		this.applier = applier;
		
		reopen();
	}
	
	public void reopen()
	{
		log = store.openMap("tx.log", new LongArrayFieldType(), new TransactionOperationType());
	}
	
	@Override
	public void accept(LogEntry item)
		throws IOException
	{
		try(ExtendedDataInput in = item.getData().asDataInput())
		{
			int msgType = in.readVInt();
			long tx = in.readVLong();
			switch(msgType)
			{
				case MessageConstants.START_TRANSACTION:
					// TODO: This should start an automatic transaction rollback timer
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
					applyTransaction(tx);
					break;
				case MessageConstants.ROLLBACK_TRANSACTION:
					removeTransaction(tx);
					break;
			}
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
		log.put(key, TransactionOperation.store(entity, id, data));
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
					}
					else
					{
						keys.add(key);
					}
					break;
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
			return new ByteArrayInputStream(log.get(key).getData());
		}
		
		
	}
}
