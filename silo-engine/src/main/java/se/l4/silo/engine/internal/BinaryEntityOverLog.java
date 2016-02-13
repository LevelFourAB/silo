package se.l4.silo.engine.internal;

import java.io.IOException;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;
import se.l4.silo.engine.SingleFetchResult;
import se.l4.silo.engine.internal.tx.TransactionExchange;

public class BinaryEntityOverLog
	implements BinaryEntity
{
	private final String name;
	private final Supplier<TransactionExchange> exchanges;
	private final StorageEntity storageEntity;

	public BinaryEntityOverLog(String name,
			Supplier<TransactionExchange> exchanges,
			StorageEntity storageEntity)
	{
		this.name = name;
		this.exchanges = exchanges;
		this.storageEntity = storageEntity;
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public StoreResult store(Object id, Bytes bytes)
	{
		TransactionExchange tx = exchanges.get();
		try
		{
			StoreResult result = tx.store(name, id, bytes);
			tx.commit();
			return result;
		}
		catch(Throwable e)
		{
			tx.rollback();
			
			Throwables.propagateIfPossible(e);
			throw Throwables.propagate(e);
		}
	}

	@Override
	public DeleteResult delete(Object id)
	{
		TransactionExchange tx = exchanges.get();
		try
		{
			DeleteResult result = tx.delete(name, id);
			tx.commit();
			return result;
		}
		catch(Throwable e)
		{
			tx.rollback();
			
			Throwables.propagateIfPossible(e);
			throw Throwables.propagate(e);
		}
	}

	@Override
	public FetchResult<BinaryEntry> get(Object id)
	{
		try
		{
			Bytes bytes = storageEntity.get(id);
			if(bytes == null)
			{
				return FetchResult.empty();
			}
			
			return new SingleFetchResult<BinaryEntry>(new BinaryEntryImpl(id, null, bytes));
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to get data with id " + id + "; " + e.getMessage(), e);
		}
	}

}
