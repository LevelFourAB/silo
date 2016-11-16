package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import se.l4.commons.id.LongIdGenerator;
import se.l4.commons.id.SequenceLongIdGenerator;
import se.l4.commons.io.Bytes;
import se.l4.commons.serialization.SerializerCollection;
import se.l4.silo.Entity;
import se.l4.silo.StorageException;
import se.l4.silo.engine.EntityCreationEncounter;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.builder.StorageBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.config.FieldConfig;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.config.QueryableEntityConfig;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.internal.mvstore.MVStoreCacheHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.internal.mvstore.SharedStorages;
import se.l4.silo.engine.log.Log;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;
import se.l4.vibe.percentile.CombinedProbes;
import se.l4.vibe.percentile.CombinedProbes.CombinedData;
import se.l4.vibe.probes.CountingProbe;
import se.l4.vibe.probes.SampledProbe;

/**
 * Storage engine implementation over a streaming log.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageEngine
	implements Closeable
{
	private static final Logger logger = LoggerFactory.getLogger(StorageEngine.class);
	
	/**
	 * Factories that can be used to fetch registered factories.
	 */
	private final EngineFactories factories;
	
	/**
	 * The {@link SerializerCollection} in use for this Silo instance.
	 */
	private SerializerCollection serializers;
	
	/**
	 * The log to use for replicating data. Created via the log builder passed
	 * to the storage engine.
	 */
	private final Log log;
	
	/**
	 * All instances of {@link Storage} that have been created by
	 * {@link Entity entities}.
	 */
	private final Map<String, StorageDef> storages;
	
	/**
	 * Instances of {@link Entity} created via configuration.
	 */
	private final Map<String, Entity> entities;
	
	/**
	 * The store used for storing main data. 
	 */
	private final MVStoreManagerImpl store;
	
	/**
	 * Abstraction over {@link MVStore} to make data storage simpler.
	 */
	private final MVDataStorage dataStorage;
	
	/**
	 * Store used for state data that is derived.  
	 */
	private final MVStoreManagerImpl stateStore;

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
	 * The adapter that receives transaction parts and turns them into
	 * storage operations.
	 */
	private final TransactionAdapter transactionAdapter;
	
	/**
	 * Helper for working with transactions.
	 */
	private final TransactionSupport transactionSupport;

	/**
	 * Root directory for data of this engine.
	 */
	private final Path root;

	/**
	 * Lock used for all mutations of this engine.
	 */
	private final Lock mutationLock;

	/**
	 * Executor for performing asynchronous tasks for this engine.
	 */
	private final ScheduledExecutorService executor;

	/**
	 * Instance of {@link SharedStorages} for use by things such as indexes.
	 */
	private final SharedStorages sharedStorages;

	private final CountingProbe stores;
	private final CountingProbe deletes;
	private final CountingProbe reads;
	
	public StorageEngine(EngineFactories factories,
			SerializerCollection serializers,
			Vibe vibe,
			TransactionSupport transactionSupport,
			LogBuilder logBuilder,
			Path root,
			EngineConfig config)
	{
		logger.debug("Creating new storage engine in {}", root);
		
		this.factories = factories;
		this.serializers = serializers;
		this.transactionSupport = transactionSupport;
		this.root = root;
		this.sharedStorages = new SharedStorages(root);
		
		mutationLock = new ReentrantLock();
	
		try
		{
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
		
		this.store = new MVStoreManagerImpl(new MVStore.Builder()
			.compress()
			.backgroundExceptionHandler((thread, t) -> {
				logger.error("Error occured in background for data store; " + t.getMessage(), t);
			})
			.fileName(root.resolve("storage.mv.bin").toString()));
		
		Path derivedState = root.resolve("derived-state.mv.bin");
		boolean hasDerivedState = Files.exists(derivedState);
		this.stateStore = new MVStoreManagerImpl(new MVStore.Builder()
			.cacheSize(4)
			.backgroundExceptionHandler((thread, t) -> {
				logger.error("Error occured in background for state store; " + t.getMessage(), t);
			})
			.compress()
			.autoCompactFillRate(20)
			.fileName(derivedState.toString()));
		
		ids = new SequenceLongIdGenerator();
		dataStorage = new MVDataStorage(this.store);
		storages = new ConcurrentHashMap<>();
		entities = new ConcurrentHashMap<>();
		
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 2, new ThreadFactoryBuilder()
			.setNameFormat("Silo Background Thread %d")
			.build()
		);
		
		stores = new CountingProbe();
		deletes = new CountingProbe();
		reads = new CountingProbe();
		
		if(vibe != null)
		{
			// Monitor the operations
			SampledProbe<CombinedData<Long>> probe = CombinedProbes.<Long>builder()
				.add("stores", stores)
				.add("deletes", deletes)
				.add("reads", reads)
				.create();
			
			vibe.sample(probe)
				.at("ops", "summary")
				.export();
			
			// Monitor our main store
			MVStore store = this.store.getStore();
			vibe.sample(MVStoreCacheHealth.createProbe(store))
				.at("store", "cache")
				.export();
			
			vibe.sample(MVStoreHealth.createProbe(store))
				.at("store", "data")
				.export();
			
			// Monitor our derived state
			store = this.stateStore.getStore();
			vibe.sample(MVStoreCacheHealth.createProbe(store))
				.at("state", "cache")
				.export();
			
			vibe.sample(MVStoreHealth.createProbe(store))
				.at("state", "data")
				.export();
		}
		
		loadConfig(config);
		
		// Build log and start receiving log entries
		transactionAdapter = new TransactionAdapter(vibe, store, createApplier());
		log = logBuilder.build(transactionAdapter);
		
		transactionLog = new TransactionLogImpl(log, ids);
		
		if(! hasDerivedState)
		{
			logger.warn("Blocking until query engines have been restored");
			// We seem to have been restored from a backup, wait for all of our query engines
			for(StorageDef def : storages.values())
			{
				def.awaitQueryEngines();
			}
			logger.info("Query engines have been restored");
		}
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
				try
				{
					stores.increase();
					mutationLock.lock();
					resolveStorage(entity).directStore(id, data);
				}
				finally
				{
					mutationLock.unlock();
				}
				
			}
			
			@Override
			public void delete(String entity, Object id) throws IOException
			{
				try
				{
					deletes.increase();
					mutationLock.lock();
					resolveStorage(entity).directDelete(id);
				}
				finally
				{
					mutationLock.unlock();
				}
			}
		};
	}
	
	@Override
	public void close()
			throws IOException
	{
		executor.shutdownNow();
		log.close();
		
		for(StorageDef def : storages.values())
		{
			def.getImpl().close();
		}
		
		store.close();
		stateStore.close();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private EntityCreationEncounter createEncounter(String name, Object config)
	{
		return new EntityCreationEncounterImpl(serializers, this, name, config);
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
			Entity entity = (Entity) factory.create(createEncounter(key, ec.as(factory.getConfigType())));
			entities.put(key, entity);
		}
		
		// TODO: Support for entity removal
	}
	
	private StorageImpl resolveStorage(String entity)
	{
		StorageDef result = storages.get(entity);
		if(result == null)
		{
			throw new StorageException("The entity " + entity + " does not exist");
		}
		return result.getImpl();
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
		StorageDef def = new StorageDef(storageName, dataPath);
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
				def.update(fields, queryEngines);
				storages.put(storageName, def);
				return def.getStorage();
			}
		};
	}
	
	public Storage getStorage(String name)
	{
		return getStorage(name, "main");
	}
	
	public Storage getStorage(String name, String subName)
	{
		String storageName = name + "::" + subName;
		StorageDef def = storages.get(storageName);
		if(def == null)
		{
			throw new StorageException("The storage " + subName + " for entity " + name + " does not exist");
		}
		return def.getStorage();
	}

	private Path resolveDataPath(String name, String subName)
	{
		return root.resolve("storage").resolve(name).resolve(subName);
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
	
	/**
	 * Create a snapshot of the data stored in this storage engine. This
	 * can be transfered to another engine or used as a backup.
	 * 
	 * @return
	 */
	public Snapshot createSnapshot()
	{
		return store.createSnapshot();
	}
	
	/**
	 * Install a snapshot into this {@link StorageEngine}.
	 * 
	 * @param snapshot
	 * @throws IOException
	 */
	public void installSnapshot(Snapshot snapshot)
		throws IOException
	{
		try
		{
			mutationLock.lock();
			
			long t1 = System.currentTimeMillis();
			logger.info("Installing a snapshot into the storage engine");
			
			// First lock all of our delegating storage instances
			for(StorageDef def : storages.values())
			{
				def.lock();
			}

			// Install the snapshot into the main engine
			store.installSnapshot(snapshot);
			
			// Destroy our derived data
			stateStore.recreate();
			
			// Reopen our data storage
			dataStorage.reopen();
			
			// Recreate all of the storages
			for(StorageDef def : storages.values())
			{
				def.recreate();
			}
			
			// Wait for all of the query engines to become up to date
			for(StorageDef def : storages.values())
			{
				def.awaitQueryEngines();
			}
			
			// Reopen our transaction adapter
			transactionAdapter.reopen();
			
			long t2 = System.currentTimeMillis();
			logger.info("Fully installed snapshot. Took " + Duration.of(t2 - t1, ChronoUnit.MILLIS).toString());
		}
		finally
		{
			mutationLock.unlock();
		}
	}
	
	private class StorageDef
	{
		private final DelegatingStorage storage;
		private final String storageName;
		private final Path dataPath;
		
		private Map<String, QueryEngineConfig> queryEngines;
		private List<FieldConfig> fieldConfigs;
		
		public StorageDef(String name, Path dataPath)
		{
			this.storageName = name;
			this.dataPath = dataPath;
			storage = new DelegatingStorage()
			{
				@Override
				public Bytes get(Object id)
				{
					reads.increase();
					return super.get(id);
				}
			};
		}
		
		public StorageImpl getImpl()
		{
			return (StorageImpl) storage.getStorage();
		}

		public DelegatingStorage getStorage()
		{
			return storage;
		}
		
		public void lock()
			throws IOException
		{
			getImpl().close();
			storage.setStorage(null);
		}
		
		public void recreate()
			throws IOException
		{
			// First delete the entire data storage of this storage
			if(Files.exists(dataPath))
			{
				Files.walkFileTree(dataPath, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException
					{
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}
	
					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc)
						throws IOException
					{
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			
			// Recreate it as we last saw it
			update(this.fieldConfigs, this.queryEngines);
		}
		
		public void awaitQueryEngines()
		{
			getImpl().awaitQueryEngines();
		}
		
		public void update(List<FieldConfig> fieldConfigs, Map<String, QueryEngineConfig> queryEngines)
		{
			if(storage.getStorage() != null)
			{
				try
				{
					// Close our existing implementation if one exists
					StorageImpl impl = (StorageImpl) storage.getStorage();
					impl.close();
				}
				catch(Throwable e)
				{
					throw new StorageException("Unable to close existing storage; " + e.getMessage(), e);
				}
			}
			
			// Create a new storage instance
			PrimaryIndex primaryIndex = new PrimaryIndex(store, ids, storageName);
			Fields fields = new FieldsImpl(Iterables.transform(fieldConfigs, c -> {
				try
				{
					String type = c.getType();
					return new FieldDefImpl(c.getName(), factories.getFieldType(type), c.isCollection());
				}
				catch(StorageException e)
				{
					throw new StorageException("Unable to create field information for " + c.getName() + " in entity " + storageName + ": " + e.getMessage());
				}
			}));
			StorageImpl impl = new StorageImpl(
				StorageEngine.this,
				sharedStorages,
				factories,
				executor,
				transactionSupport,
				stateStore,
				dataPath,
				dataStorage,
				primaryIndex,
				storageName,
				fields,
				queryEngines
			);
			
			// Save the configuration for later usage
			this.fieldConfigs = fieldConfigs;
			this.queryEngines = queryEngines;
			
			// Set the implementation used
			storage.setStorage(impl);
		}
	}
}
