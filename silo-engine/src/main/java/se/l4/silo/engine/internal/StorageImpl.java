package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.internal.query.QueryEncounterImpl;
import se.l4.silo.engine.internal.query.QueryEngineCreationEncounterImpl;
import se.l4.silo.engine.internal.query.QueryEngineUpdater;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryResult;

/**
 * Implementation of {@link Storage}.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageImpl
	implements Storage, Closeable
{
	private final String name;
	private final TransactionSupport transactionSupport;
	private final DataStorage storage;
	private final PrimaryIndex primary;
	private final Fields fields;
	
	private final ImmutableMap<String, QueryEngine<?>> queryEngines;
	private final QueryEngineUpdater queryEngineUpdater;

	public StorageImpl(
			EngineFactories factories,
			ScheduledExecutorService executor,
			TransactionSupport transactionSupport,
			MVStoreManager stateStore,
			Path dataDir,
			DataStorage storage,
			PrimaryIndex primary,
			String name,
			Fields fields,
			Map<String, QueryEngineConfig> queryEngines)
	{
		this.name = name;
		this.transactionSupport = transactionSupport;
		this.storage = storage;
		this.primary = primary;
		this.fields = fields;
		
		ImmutableMap.Builder<String, QueryEngine<?>> builder = ImmutableMap.builder();
		for(Map.Entry<String, QueryEngineConfig> qe : queryEngines.entrySet())
		{
			String key = qe.getKey();
			QueryEngineConfig config = qe.getValue();
			
			String type = config.getType();
			QueryEngineFactory<?, ?> factory = factories.forQueryEngine(type);
			QueryEngine<?> engine = factory.create(new QueryEngineCreationEncounterImpl(
				dataDir,
				key,
				config.as(factory.getConfigClass()),
				fields
			));
			
			builder.put(key, engine);
		}
		
		this.queryEngines = builder.build();
		this.queryEngineUpdater = new QueryEngineUpdater(stateStore, this, executor, name, this.queryEngines);
	}
	
	@Override
	public void close()
		throws IOException
	{
		// Close all of our query engines
		for(QueryEngine<?> engine : this.queryEngines.values())
		{
			engine.close();
		}
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
	
	public Bytes getInternal(long id)
	{
		try
		{
			return storage.get(id);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to get internal data with id " + id + "; " + e.getMessage(), e);
		}
	}
	
	public long getLatest()
	{
		return primary.latest();
	}
	
	public long nextId(long id)
	{
		return primary.nextAfter(id);
	}
	
	@Override
	public <R> QueryFetchResult<QueryResult<R>> query(String engine, Object query, Function<Bytes, R> dataLoader)
	{
		QueryEngine<?> qe = queryEngines.get(engine);
		if(qe == null)
		{
			throw new StorageException("Unknown query engine `" + engine + "`");
		}
		
		QueryEncounterImpl encounter = new QueryEncounterImpl<>(query, id -> {
			try
			{
				Bytes data = storage.get(id);
				return dataLoader.apply(data);
			}
			catch(IOException e)
			{
				throw new StorageException("Unable to fetch data for internal id " + id + "; " + e.getMessage(), e);
			}
		});
		
		qe.query(encounter);
		
		return encounter.getResult();
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
		long previous = primary.latest();
		long internalId = primary.store(id);
		storage.store(internalId, bytes);
		
		Bytes storedBytes = storage.get(internalId);
		
		queryEngineUpdater.store(previous, internalId, storedBytes);
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
		
		queryEngineUpdater.delete(internalId);
		
		storage.delete(internalId);
		primary.remove(id);
	}

	public void awaitQueryEngines()
	{
		while(! queryEngineUpdater.isAllUpDate())
		{
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new StorageException("Interrupted while waiting for query engines to be updated");
			}
		}
	}
}
