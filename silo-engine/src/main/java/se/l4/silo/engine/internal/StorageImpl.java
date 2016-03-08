package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.config.QueryEngineConfig;
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
	private final ImmutableMap<String, QueryEngine<?>> queryEngines;
	private final Fields fields;

	public StorageImpl(
			EngineFactories factories,
			TransactionSupport transactionSupport,
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
	
	@Override
	public void query(String engine, Object query)
	{
		QueryEngine<?> qe = queryEngines.get(engine);
		System.out.println("Should invoke " + query + " on " + qe);
		System.out.println("  " + query);
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
		
		Bytes storedBytes = storage.get(internalId);
		
		// TODO: Keep track of the last id of each query engine
		for(Map.Entry<String, QueryEngine<?>> e : queryEngines.entrySet())
		{
			QueryEngine<?> engine = e.getValue();
			engine.update(internalId, new DataEncounterImpl(storedBytes));
		}
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
