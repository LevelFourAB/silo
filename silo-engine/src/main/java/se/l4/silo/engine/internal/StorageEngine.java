package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.h2.mvstore.MVStore;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

import se.l4.aurochs.core.id.LongIdGenerator;
import se.l4.aurochs.core.id.SimpleLongIdGenerator;
import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.Entity;
import se.l4.silo.StorageException;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.EntityCreationEncounter;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.builder.StorageBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.config.FieldConfig;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.config.QueryableEntityConfig;
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
	/**
	 * Factories that can be used to fetch registered factories.
	 */
	private final EngineFactories factories;
	
	/**
	 * The log to use for replicating data. Created via the log builder passed
	 * to the storage engine.
	 */
	private final Log log;
	
	/**
	 * All instances of {@link Storage} that have been created by
	 * {@link Entity entities}.
	 */
	private final Map<String, StorageImpl> storages;
	
	/**
	 * Instances of {@link Entity} created via configuration.
	 */
	private final Map<String, Entity> entities;
	
	/**
	 * The store used for storing main data. 
	 */
	private final MVStoreManager store;
	/**
	 * Abstraction over {@link MVStore} to make data storage simpler.
	 */
	private final DataStorage dataStorage;

	/**
	 * The generator used for creating identifiers for transactions and
	 * primary key mapping.
	 */
	private final LongIdGenerator ids;

	/**
	 * The log used for mapping transaction operations to smaller chunks
	 * that can be passed to {@link #log}.
	 */
	private final TransactionLog transactionLog;
	/**
	 * Helper for working with transactions.
	 */
	private final TransactionSupport transactionSupport;

	/**
	 * Root directory for data of this engine.
	 */
	private final Path root;

	public StorageEngine(EngineFactories factories,
			TransactionSupport transactionSupport,
			LogBuilder logBuilder,
			Path root,
			EngineConfig config)
	{
		this.factories = factories;
		this.transactionSupport = transactionSupport;
		this.root = root;
	
		try
		{
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
		
		MVStore store = new MVStore.Builder()
			.compress()
			.fileName(root.resolve("storage.mv.bin").toString())
			.open();
		
		this.store = new MVStoreManagerImpl(store);
		
		ids = new SimpleLongIdGenerator();
		dataStorage = new MVDataStorage(this.store);
		storages = new ConcurrentHashMap<>();
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
				resolveStorage(entity).directStore(id, data);
				
			}
			
			@Override
			public void delete(String entity, Object id) throws IOException
			{
				resolveStorage(entity).directDelete(id);
			}
		};
	}
	
	@Override
	public void close()
			throws IOException
	{
		log.close();
		store.close();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private EntityCreationEncounter createEncounter(String name, Object config)
	{
		return new EntityCreationEncounterImpl(this, name, config);
	}
	
	/**
	 * Load the given configuration.
	 * 
	 * @param config
	 */
	@SuppressWarnings("unchecked")
	private void loadConfig(EngineConfig config)
	{
		for(Map.Entry<String, EntityConfig> entry : config.getEntities().entrySet())
		{
			String key = entry.getKey();
			EntityConfig ec = entry.getValue();
			
			// TODO: Check if the configuration has actually changed for existing entities before recreating
			
			EntityTypeFactory<?, ?> factory = factories.forEntity(ec.getType());
			Entity entity = factory.create(createEncounter(key, ec.as(factory.getConfigType())));
			entities.put(key, entity);
		}
		
		// TODO: Support for entity removal
	}
	
	private StorageImpl resolveStorage(String entity)
	{
		StorageImpl result = storages.get(entity);
		if(result == null)
		{
			throw new StorageException("The entity " + entity + " does not exist");
		}
		return result;
	}
	
	/**
	 * Get the {@link TransactionLog} this engine uses.
	 * 
	 * @return
	 */
	public TransactionLog getTransactionLog()
	{
		return transactionLog;
	}
	
	/**
	 * Create a new main storage.
	 * 
	 * @param name
	 * @return
	 */
	public StorageBuilder createStorage(String name)
	{
		return createStorage(name, "main");
	}

	/**
	 * Create a new storage.
	 * 
	 * @param name
	 * @param subName
	 * @return
	 */
	public StorageBuilder createStorage(String name, String subName)
	{
		String storageName = name + "::" + subName;
		Path dataPath = resolveDataPath(name, subName);
		return new StorageBuilder()
		{
			private final Map<String, QueryEngineConfig> queryEngines = new HashMap<>();
			private final List<FieldConfig> fields = new ArrayList<>();
			
			@Override
			public StorageBuilder withFields(Iterable<FieldConfig> fields)
			{
				for(FieldConfig f : fields)
				{
					this.fields.add(f);
				}
				return this;
			}
			
			@Override
			public StorageBuilder withQueryEngines(QueryableEntityConfig config)
			{
				queryEngines.putAll(config.getQueryEngines());
				return this;
			}
			
			@Override
			public <C extends QueryEngineConfig> StorageBuilder withQueryEngine(QueryEngineFactory<?, C> factory, C config)
			{
				queryEngines.put(factory.getId(), config);
				return this;
			}
			
			@Override
			public Storage build()
			{
				StorageImpl storage = storages.get(storageName);
				if(storage == null)
				{
					// New storage, create the instance
					PrimaryIndex primaryIndex = new PrimaryIndex(store, ids, storageName);
					Fields fields = new FieldsImpl(Iterables.transform(this.fields, c -> {
						String type = c.getType();
						return new FieldDefImpl(c.getName(), factories.getFieldType(type), c.isCollection());
					}));
					storage = new StorageImpl(
						factories,
						transactionSupport,
						dataPath,
						dataStorage,
						primaryIndex,
						storageName,
						fields,
						queryEngines
					);
					storages.put(storageName, storage);
					return storage;
				}
				
				// TODO: Reload configuration of storage
				
				return storage;
			}
		};
	}

	private Path resolveDataPath(String name, String subName)
	{
		return root.resolve(name).resolve(subName);
	}

	/**
	 * Get an entity from this engine.
	 * 
	 * @param entityName
	 * @return
	 */
	public <T> T getEntity(String entityName)
	{
		return (T) entities.get(entityName);
	}
}
