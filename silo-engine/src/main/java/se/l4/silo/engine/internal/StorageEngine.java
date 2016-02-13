package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.h2.mvstore.MVStore;

import com.google.common.base.Throwables;

import se.l4.aurochs.core.id.SimpleLongIdGenerator;
import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.StorageException;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.log.Log;
import se.l4.silo.engine.log.LogBuilder;

/**
 * Storage engine implementation over a streaming log.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageEngine
	implements Closeable
{
	private final Log log;
	
	private final Map<String, StorageEntity> entities;
	
	private final MVStore store;
	private final MVDataStorage storage;

	private final SimpleLongIdGenerator ids;

	private final TransactionLog transactionLog;

	private final EntityChangeListener entityListener;
	
	public StorageEngine(LogBuilder logBuilder, Path root, EngineConfig config,
			EntityChangeListener entityListener)
	{
		this.entityListener = entityListener;
	
		try
		{
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
		
		store = new MVStore.Builder()
			.compress()
			.fileName(root.resolve("storage.mv.bin").toString())
			.open();
		
		ids = new SimpleLongIdGenerator();
		storage = new MVDataStorage(store);
		entities = new ConcurrentHashMap<>();
		
		loadConfig(config);
		
		// Build log and start receiving log entries
		TransactionAdapter adapter = new TransactionAdapter(store, createApplier());
		log = logBuilder.build(adapter);
		
		transactionLog = new TransactionLogImpl(log, ids);
	}
	
	/**
	 * Create the instance of {@link StorageApplier} that is used for this
	 * engine.
	 * 
	 * @return
	 */
	private StorageApplier createApplier()
	{
		return new StorageApplier()
		{
			@Override
			public void store(String entity, Object id, Bytes data) throws IOException
			{
				resolveEntity(entity).store(id, data);
				
			}
			
			@Override
			public void delete(String entity, Object id) throws IOException
			{
				resolveEntity(entity).delete(id);
			}
		};
	}
	
	/**
	 * Load the given configuration.
	 * 
	 * @param config
	 */
	private void loadConfig(EngineConfig config)
	{
		for(Map.Entry<String, EntityConfig> entry : config.getEntities().entrySet())
		{
			String key = entry.getKey();
			EntityConfig ec = entry.getValue();
			
			StorageEntity existing = entities.get(key);
			if(existing == null)
			{
				StorageEntity newEntity = createEntity(key, ec);
				entities.put(key, newEntity);
				entityListener.newBinaryEntity(key, newEntity);
			}
			else
			{
				existing.loadConfig(ec);
			}
		}
		
		// TODO: Support for entity removal
	}
	
	/**
	 * Create an entity that 
	 * @param key
	 * @param ec
	 * @return
	 */
	private StorageEntity createEntity(String key, EntityConfig ec)
	{
		PrimaryIndex primaryIndex = new PrimaryIndex(store, ids, key);
		return new StorageEntity(storage, primaryIndex);
	}

	private StorageEntity resolveEntity(String entity)
	{
		StorageEntity result = entities.get(entity);
		if(result == null)
		{
			throw new StorageException("The entity " + entity + " does not exist");
		}
		return result;
	}
	
	public TransactionLog getTransactionLog()
	{
		return transactionLog;
	}
	
	@Override
	public void close()
		throws IOException
	{
		log.close();
		store.close();
	}
}
