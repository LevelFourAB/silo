package se.l4.silo.engine.internal;

import java.util.function.Function;

import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.Storage;
import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryResult;
import se.l4.ylem.io.Bytes;

/**
 * {@link Storage} that delegates to another instance. This is what is
 * exposed to {@link Entity entity implementations}. This allows the
 * {@link StorageEngine} to switch the underlying storage when needed,
 * for example when a {@link Snapshot} is being installed.
 *
 * @author Andreas Holstenson
 *
 */
public class DelegatingStorage
	implements Storage
{
	private volatile Storage storage;

	public DelegatingStorage()
	{
	}

	public Storage getStorage()
	{
		return storage;
	}

	public void setStorage(Storage storage)
	{
		this.storage = storage;
	}

	private void check()
	{
		if(storage == null)
		{
			throw new StorageException("Storage instance is currently locked and can not be used");
		}
	}

	@Override
	public StoreResult store(Object id, Bytes bytes)
	{
		check();
		return storage.store(id, bytes);
	}

	@Override
	public Bytes get(Object id)
	{
		check();
		return storage.get(id);
	}

	@Override
	public DeleteResult delete(Object id)
	{
		check();
		return storage.delete(id);
	}

	@Override
	public <R> QueryFetchResult<QueryResult<R>> query(String engine, Object query, Function<Bytes, R> dataLoader)
	{
		check();
		return storage.query(engine, query, dataLoader);
	}

	@Override
	public FetchResult<Bytes> stream()
	{
		check();
		return storage.stream();
	}
}
