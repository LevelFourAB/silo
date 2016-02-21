package se.l4.silo.engine.internal;

import java.io.IOException;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.internal.tx.TransactionExchange;

/**
 * Implementation of {@link Storage}.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageImpl
	implements Storage
{
	private final String name;
	private final TransactionSupport transactionSupport;
	private final DataStorage storage;
	private final PrimaryIndex primary;

	public StorageImpl(String name,
			TransactionSupport transactionSupport,
			DataStorage storage,
			PrimaryIndex primary)
	{
		this.name = name;
		this.transactionSupport = transactionSupport;
		this.storage = storage;
		this.primary = primary;
	}

	@Override
	public StoreResult store(Object id, Bytes bytes)
	{
		TransactionExchange tx = transactionSupport.getExchange();
		try
		{
			StoreResult result = tx.store(name, id, bytes);
			tx.commit();
			return result;
		}
		catch(Throwable e)
		{
			tx.rollback();
			
			throw new StorageException("Unable to store data with id " + id + "; " + e.getMessage(), e);
		}
	}

	@Override
	public DeleteResult delete(Object id)
	{
		TransactionExchange tx = transactionSupport.getExchange();
		try
		{
			DeleteResult result = tx.delete(name, id);
			tx.commit();
			return result;
		}
		catch(Throwable e)
		{
			tx.rollback();
			
			throw new StorageException("Unable to delete data with id " + id + "; " + e.getMessage(), e);
		}
	}
	
	@Override
	public Bytes get(Object id)
	{
		long internalId = primary.get(id);
		if(internalId == 0) return null;
		
		try
		{
			return storage.get(internalId);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to get data with id " + id + "; " + e.getMessage(), e);
		}
	}

	/**
	 * Store an entry for this entity.
	 * 
	 * @param id
	 * @param bytes
	 * @throws IOException
	 */
	public void directStore(Object id, Bytes bytes)
		throws IOException
	{
		long internalId = primary.store(id);
		storage.store(internalId, bytes);
	}
	
	/**
	 * Delete a previously stored entry.
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void directDelete(Object id)
		throws IOException
	{
		long internalId = primary.get(id);
		if(internalId == 0) return;
		
		storage.delete(internalId);
		primary.remove(id);
	}
}
