package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import se.l4.silo.Entity;
import se.l4.silo.StorageException;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.IndexDefinition;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.Storage.Builder;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.internal.mvstore.MVStoreCacheHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.internal.mvstore.SharedStorages;
import se.l4.silo.engine.internal.tx.LogBasedTransactionSupport;
import se.l4.silo.engine.log.Log;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;
import se.l4.vibe.operations.Change;
import se.l4.vibe.probes.CountingProbe;
import se.l4.vibe.probes.SampledProbe;
import se.l4.vibe.snapshots.MapSnapshot;
import se.l4.ylem.ids.LongIdGenerator;
import se.l4.ylem.ids.SequenceLongIdGenerator;

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
	private final ImmutableMap<String, Entity<?, ?>> entities;

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

	public StorageEngine(
		Vibe vibe,
		LogBuilder logBuilder,
		Path root,

		EngineConfig config,
		ListIterable<EntityDefinition> entities
	)
	{
		logger.debug("Creating new storage engine in {}", root);

		this.root = root;
		this.sharedStorages = new SharedStorages(root, vibe);

		mutationLock = new ReentrantLock();

		try
		{
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not create initial directory; " + e.getMessage(), e);
		}

		this.store = new MVStoreManagerImpl(new MVStore.Builder()
			.compress()
			.backgroundExceptionHandler((thread, t) -> {
				logger.error("Error occured in background for data store; " + t.getMessage(), t);
			})
			.cacheSize(config.getCacheSizeInMb())
			.fileName(root.resolve("storage.mv.bin").toString()));

		Path derivedState = root.resolve("derived-state.mv.bin");
		boolean hasDerivedState = Files.exists(derivedState);
		this.stateStore = new MVStoreManagerImpl(new MVStore.Builder()
			.cacheSize(4)
			.backgroundExceptionHandler((thread, t) -> {
				logger.error("Error occured in background for state store; " + t.getMessage(), t);
			})
			.autoCompactFillRate(20)
			.fileName(derivedState.toString()));

		ids = new SequenceLongIdGenerator();
		storages = new ConcurrentHashMap<>();

		executor = Executors.newScheduledThreadPool(
			Runtime.getRuntime().availableProcessors() + 2,
			new ThreadFactory()
			{
				private final AtomicInteger count = new AtomicInteger(0);

				public Thread newThread(Runnable runnable)
				{
					return new Thread(runnable, "Silo Background " + count.incrementAndGet());
				}
			}
		);

		stores = new CountingProbe();
		deletes = new CountingProbe();
		reads = new CountingProbe();

		if(vibe != null)
		{
			// Monitor the operations
			SampledProbe<MapSnapshot> probe = SampledProbe.merged()
				.add("stores", stores.apply(Change.changeAsLong()))
				.add("deletes", deletes.apply(Change.changeAsLong()))
				.add("reads", reads.apply(Change.changeAsLong()))
				.build();

			vibe.export(probe)
				.at("ops", "summary")
				.done();

			// Monitor our main store
			MVStore store = this.store.getStore();
			vibe.export(MVStoreCacheHealth.createProbe(store))
				.at("store", "cache")
				.done();

			vibe.export(MVStoreHealth.createProbe(store))
				.at("store", "data")
				.done();

			// Monitor our derived state
			store = this.stateStore.getStore();
			vibe.export(MVStoreCacheHealth.createProbe(store))
				.at("state", "cache")
				.done();

			vibe.export(MVStoreHealth.createProbe(store))
				.at("state", "data")
				.done();
		}

		// Build log and start receiving log entries
		transactionAdapter = new TransactionAdapter(vibe, executor, store, createApplier());
		log = logBuilder.build(transactionAdapter);

		transactionLog = new TransactionLogImpl(log, ids);
		transactionSupport = new LogBasedTransactionSupport(store, transactionLog);

		dataStorage = new MVDataStorage(store, transactionSupport);

		this.entities = createEntities(entities);

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
			public void store(String entity, Object id, InputStream data)
				throws IOException
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
			public void delete(String entity, Object id)
				throws IOException
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

			@Override
			public void index(String entity, String index, Object id, InputStream data)
				throws IOException
			{
				try
				{
					mutationLock.lock();
					resolveStorage(entity).directIndex(index, id, data);
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

		try
		{
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch(InterruptedException e)
		{
			// Ignore the interruption
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private final ImmutableMap<String, Entity<?, ?>> createEntities(ListIterable<EntityDefinition> entities)
	{
		// TODO: Support for different entity implementations?
		return (ImmutableMap) entities.collect(def -> {
			return new EntityImpl(
				def.getName(),
				def.getIdSupplier(),
				createStorage(def.getName(), def.getCodec())
					.addIndexes(def.getIndexes())
					.build()
			);
		}).toMap(v -> v.getName(), v -> v).toImmutable();
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
	 * Get the {@link TransactionSupport} this engine uses.
	 *
	 * @return
	 */
	public TransactionSupport getTransactionSupport()
	{
		return transactionSupport;
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
	public <T> Storage.Builder<T> createStorage(String name, EntityCodec<T> codec)
	{
		return createStorage(name, "main", codec);
	}

	/**
	 * Create a new storage.
	 *
	 * @param name
	 * @param subName
	 * @return
	 */
	public <T> Storage.Builder<T> createStorage(String name, String subName, EntityCodec<T> codec)
	{
		String storageName = name + "::" + subName;
		Path dataPath = resolveDataPath(name, subName);
		StorageDef<T> def = new StorageDef<>(storageName, dataPath, codec);
		return new Storage.Builder<T>()
		{
			private final MutableList<IndexDefinition<T>> indexes = Lists.mutable.empty();

			@Override
			public Builder<T> addIndexes(Iterable<IndexDefinition<T>> indexes)
			{
				this.indexes.withAll(indexes);
				return this;
			}

			@Override
			public Builder<T> addIndex(IndexDefinition<T> index)
			{
				this.indexes.add(index);
				return this;
			}

			@Override
			public Storage<T> build()
			{
				def.update(indexes);
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
	public Entity<?, ?> getEntity(String entityName)
	{
		return entities.get(entityName);
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

	public void compact(long time, TimeUnit unit)
	{
		store.compact(unit.toMillis(time));
	}

	private class StorageDef<T>
	{
		private final DelegatingStorage<T> storage;
		private final String storageName;
		private final Path dataPath;

		private final EntityCodec<T> codec;
		private ListIterable<IndexDefinition<T>> queryEngines;

		public StorageDef(
			String name,
			Path dataPath,
			EntityCodec<T> codec
		)
		{
			this.storageName = name;
			this.dataPath = dataPath;
			this.codec = codec;

			storage = new DelegatingStorage<T>()
			{
				@Override
				public Mono<T> get(Object id)
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
			update(this.queryEngines);
		}

		public void awaitQueryEngines()
		{
			getImpl().awaitQueryEngines();
		}

		public void update(
			ListIterable<IndexDefinition<T>> queryEngines
		)
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
			PrimaryIndex primaryIndex = new PrimaryIndex(
				store,
				transactionSupport,
				storageName
			);

			StorageImpl impl = new StorageImpl(
				StorageEngine.this,
				sharedStorages,
				executor,
				transactionSupport,

				store,
				stateStore,
				dataPath,
				dataStorage,

				storageName,
				codec,

				primaryIndex,
				queryEngines
			);

			// Save the configuration for later usage
			this.queryEngines = queryEngines;

			// Set the implementation used
			storage.setStorage(impl);
		}
	}
}
