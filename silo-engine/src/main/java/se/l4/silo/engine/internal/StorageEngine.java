package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import se.l4.silo.Collection;
import se.l4.silo.StorageException;
import se.l4.silo.StorageTransactionException;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.ObjectCodec;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.index.IndexDef;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.internal.migration.Migration;
import se.l4.silo.engine.internal.mvstore.MVStoreCacheHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreHealth;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.internal.mvstore.SharedStorages;
import se.l4.silo.engine.internal.tx.LogBasedTransactionSupport;
import se.l4.silo.engine.internal.tx.TransactionLogApplier;
import se.l4.silo.engine.internal.tx.TransactionSupport;
import se.l4.silo.engine.internal.tx.TransactionWaiter;
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
	 * {@link Collection collections}.
	 */
	private final Map<String, StorageImpl<?>> storages;

	/**
	 * The store used for storing main data.
	 */
	private final MVStoreManagerImpl store;

	/**
	 * {@link DataStorage} used for collection data.
	 */
	private final MVDataStorage dataStorage;

	/**
	 * {@link DataStorage} used for index data.
	 */
	private final MVDataStorage indexDataStorage;

	/**
	 * The generator used for creating identifiers for transactions.
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
	private final TransactionLogApplier transactionAdapter;

	/**
	 * Helper for working with transactions.
	 */
	private final TransactionSupport transactionSupport;

	/**
	 * Helper used to wait for a transaction being applied.
	 */
	private final TransactionWaiterImpl transactionWaiter;

	/**
	 * Root directory for data of this engine.
	 */
	private final Path root;

	/**
	 * Lock used for all mutations of this engine.
	 */
	private final Lock mutationLock;

	/**
	 * Scheduler for performing asynchronous tasks for this engine.
	 */
	private final Scheduler scheduler;

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
		ListIterable<CollectionDef> collectionDefs
	)
	{
		logger.debug("Creating new storage engine in {}", root);

		this.root = root;

		scheduler = Schedulers.newBoundedElastic(
			Runtime.getRuntime().availableProcessors() + 2,
			Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
			"silo"
		);

		this.sharedStorages = new SharedStorages(scheduler, root, vibe);

		mutationLock = new ReentrantLock();

		try
		{
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not create initial directory; " + e.getMessage(), e);
		}

		this.store = new MVStoreManagerImpl(scheduler, new MVStore.Builder()
			.compress()
			.backgroundExceptionHandler((thread, t) -> {
				logger.error("Error occured in background for data store; " + t.getMessage(), t);
			})
			.cacheSize(config.getCacheSizeInMiB())
			.cacheConcurrency(config.getCacheConcurrency())
			.autoCompactFillRate(config.getAutoCompactFillRate())
			.autoCommitBufferSize(config.getAutoCommitBufferSizeInKiB())
			.fileName(root.resolve("storage.mv.bin").toString()));

		// Request a migration of the store
		Migration.migrate(store.getStore());

		ids = new SequenceLongIdGenerator();
		storages = new ConcurrentHashMap<>();

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
		}

		// Build log and start receiving log entries
		transactionAdapter = new TransactionLogApplier(vibe, scheduler, store, createApplier());
		log = logBuilder.build(transactionAdapter);

		transactionWaiter = new TransactionWaiterImpl();
		transactionLog = new TransactionLogImpl(log, ids);
		transactionSupport = new LogBasedTransactionSupport(
			store,
			transactionLog,
			mutationLock,
			transactionWaiter
		);

		dataStorage = new MVDataStorage("data", store);
		dataStorage.provideTransactionValues(transactionSupport::registerValue);

		indexDataStorage = new MVDataStorage("index.data", store);

		// FIXME: Policies for waiting for query engines
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
			public void transactionStart(long id)
			{
				mutationLock.lock();
			}

			@Override
			public void store(String collection, Object id, InputStream data)
				throws IOException
			{
				stores.increase();

				StorageImpl storage = storages.get(collection);
				if(storage == null)
				{
					return;
				}

				storage.directStore(id, data);
			}

			@Override
			public void delete(String collection, Object id)
				throws IOException
			{
				deletes.increase();
				mutationLock.lock();

				StorageImpl storage = storages.get(collection);
				if(storage == null)
				{
					return;
				}

				storage.directDelete(id);
			}

			@Override
			public void index(String collection, String index, Object id, InputStream data)
				throws IOException
			{
				StorageImpl storage = storages.get(collection);
				if(storage == null)
				{
					return;
				}

				storage.directIndex(index, id, data);
			}

			@Override
			public void transactionComplete(long id, Throwable throwable)
			{
				mutationLock.unlock();
				transactionWaiter.complete(id);
			}
		};
	}

	@Override
	public void close()
			throws IOException
	{
		log.close();

		for(StorageImpl storage : storages.values())
		{
			storage.close();
		}

		scheduler.dispose();
		store.close();
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
	 * Create a new storage.
	 *
	 * @param name
	 * @return
	 */
	public <T> Storage.Builder<T> createStorage(String name, ObjectCodec<T> codec)
	{
		String storageName = name;
		return new Storage.Builder<T>()
		{
			private final MutableList<IndexDef<T>> indexes = Lists.mutable.empty();

			@Override
			public Storage.Builder<T> addIndexes(Iterable<IndexDef<T>> indexes)
			{
				this.indexes.withAll(indexes);
				return this;
			}

			@Override
			public Storage.Builder<T> addIndex(IndexDef<T> index)
			{
				this.indexes.add(index);
				return this;
			}

			@Override
			public Storage<T> build()
			{
				// Create a new storage instance
				StorageImpl storage = new StorageImpl(
					StorageEngine.this,
					sharedStorages,
					scheduler,
					transactionSupport,

					store,
					dataStorage,

					storageName,
					codec,

					indexDataStorage,
					root.resolve("index").resolve(name),
					indexes
				);

				storages.put(storageName, storage);

				return storage;
			}
		};
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

	public void compact(long timeInMillis)
	{
		store.compact(timeInMillis);
	}

	/**
	 * Implementation of {@link TransactionWaiter} that uses latches to wait
	 * for transactions.
	 */
	private static class TransactionWaiterImpl
		implements TransactionWaiter
	{
		private final ConcurrentHashMap<Long, CountDownLatch> latches;

		public TransactionWaiterImpl()
		{
			latches = new ConcurrentHashMap<>();
		}

		/**
		 * Complete the given transaction.
		 *
		 * @param tx
		 */
		public void complete(long tx)
		{
			/*
			 * Remove the latch associated with the given transaction and if
			 * it's available count it down.
			 */
			CountDownLatch latch = latches.remove(tx);
			if(latch != null)
			{
				latch.countDown();
			}
		}

		@Override
		public Mono<Void> getWaiter(long tx)
		{
			/*
			 * This is called from {@link LogBasedTransactionSupport} to
			 * retrieve a mono that will represent the end of a transaction.
			 *
			 * Create a latch, store it and return a mono that will await it.
			 */
			CountDownLatch latch = new CountDownLatch(1);
			latches.put(tx, latch);

			return Mono.fromRunnable(() -> {
				try
				{
					latch.await();
				}
				catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					throw new StorageTransactionException("Aborted waiting for transaction, thread was interrupted", e);
				}
			});
		}
	}
}
