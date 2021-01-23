package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.silo.engine.index.LocalIndex;
import se.l4.silo.engine.internal.index.IndexEngineController;
import se.l4.silo.engine.internal.index.IndexEngineCreationEncounterImpl;
import se.l4.silo.engine.internal.index.IndexEngineRebuildEncounter;
import se.l4.silo.engine.internal.index.IndexQueryEncounterImpl;
import se.l4.silo.engine.internal.mvstore.SharedStorages;
import se.l4.silo.engine.internal.tx.TransactionSupport;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;
import se.l4.silo.index.Query;

/**
 * Implementation of {@link Storage}.
 */
public class StorageImpl<T>
	implements Storage<T>, Closeable
{
	private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);
	private final TransactionSupport transactionSupport;
	private final DataStorage mainDataStorage;

	private final String name;
	private final EntityCodec<T> codec;

	private final PrimaryIndex primary;
	private final MapIterable<String, Index<T, ?>> queryEngines;
	private final MapIterable<String, IndexEngineController<T, ?>> queryControllers;

	private final Iterable<Disposable> disposables;

	private final LongAdder reads;
	private final LongAdder stores;
	private final LongAdder deletes;

	public StorageImpl(
		StorageEngine engine,
		SharedStorages storages,

		Scheduler scheduler,
		TransactionSupport transactionSupport,

		MVStoreManager store,
		DataStorage mainDataStorage,

		String name,
		EntityCodec<T> codec,

		DataStorage indexDataStorage,
		Path indexDataPath,
		RichIterable<IndexDefinition<?>> indexes
	)
	{
		this.name = name;
		this.transactionSupport = transactionSupport;
		this.mainDataStorage = mainDataStorage;
		this.primary = new PrimaryIndex(
			store,
			transactionSupport,
			name
		);

		this.codec = codec;

		reads = new LongAdder();
		stores = new LongAdder();
		deletes = new LongAdder();

		this.queryEngines = (ImmutableMap) indexes.toMap(IndexDefinition::getName, def -> {
			String key = def.getName();
			return def.create(new IndexEngineCreationEncounterImpl(
				storages,
				scheduler,
				indexDataPath,
				key,
				name + "-" + key
			));
		}).toImmutable();

		this.queryControllers = this.queryEngines.collectValues((key, e) -> new IndexEngineController<>(
			store,
			mainDataStorage,
			e,
			name + "-" + key
		));

		// Register transactional values used by query engines
		queryControllers
			.each(q -> q.provideTransactionValues(transactionSupport::registerValue));

		// Start each of the indexes using the executor
		long largestId = primary.latest();
		long size = primary.size();
		IndexEngineRebuildEncounter<T> rebuild = new IndexEngineRebuildEncounter<T>()
		{
			public long getSize()
			{
				return size;
			}

			@Override
			public long getLargestId()
			{
				return largestId;
			}

			public Iterator<LongObjectPair<T>> iterator(long minIdExclusive, long maxIdInclusive)
			{
				return createIterator(minIdExclusive, maxIdInclusive);
			}
		};

		disposables = Flux.fromIterable(queryControllers)
			.parallel()
			.runOn(scheduler)
			.flatMap(c -> c.start(rebuild))
			.sequential()
			.collectList()
			.block();
	}

	@Override
	public long getReads()
	{
		return reads.sum();
	}

	@Override
	public long getStores()
	{
		return stores.sum();
	}

	@Override
	public long getDeletes()
	{
		return deletes.sum();
	}

	@Override
	public Mono<LocalIndex> getIndex(String name)
	{
		return Mono.justOrEmpty(queryControllers.get(name));
	}

	@Override
	public Flux<LocalIndex> indexes()
	{
		return Flux.fromIterable(queryControllers);
	}

	@Override
	public void close()
		throws IOException
	{
		// Close the controllers
		for(Disposable d : disposables)
		{
			d.dispose();
		}

		// Close all of our query engines
		for(Index<?, ?> engine : this.queryEngines)
		{
			engine.close();
		}
	}

	@Override
	public <ID> Mono<StoreResult<ID, T>> store(ID id, T instance)
	{
		return transactionSupport.withExchange(tx -> {
			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] TX store of " + id);
			}

			try
			{
				// Encode the main object
				tx.store(name, id, out -> codec.encode(instance, out));

				// Generate index data for the object
				for(IndexEngineController<T, ?> controller : queryControllers)
				{
					tx.index(name, controller.getName(), id, out -> {
						controller.generate(instance, out);
					});
				}

				return new StoreResultImpl<>(
					id,
					instance
				);
			}
			catch(Throwable e)
			{
				throw new StorageException("Unable to store data with id " + id + "; " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <ID> Mono<DeleteResult<ID, T>> delete(ID id)
	{
		return transactionSupport.withExchange(tx -> {
			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] TX delete of " + id);
			}

			long internalId = primary.get(tx, id);
			if(internalId == 0)
			{
				return new DeleteResultImpl<>(id, false);
			}

			try
			{
				tx.delete(name, id);
				return new DeleteResultImpl<ID,T>(id, true);
			}
			catch(Throwable e)
			{
				throw new StorageException("Unable to delete data with id " + id + "; " + e.getMessage(), e);
			}
		}, primary);
	}

	@Override
	public Mono<T> get(Object id)
	{
		return transactionSupport.withExchange(tx -> {
			long internalId = primary.get(tx, id);

			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] Getting " + id + " mapped to internal id " + internalId);
			}

			if(internalId == 0) return null;

			return getInternal(tx, internalId);
		}, mainDataStorage, primary);
	}

	public T getInternal(WriteableTransactionExchange exchange, long id)
	{
		try
		{
			reads.increment();

			try(InputStream in = mainDataStorage.get(exchange, id);)
			{
				return codec.decode(in);
			}
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

	public int size()
	{
		return primary.size();
	}

	private IndexQueryEncounterImpl createQueryEncounter(
		WriteableTransactionExchange tx,
		Query query
	)
	{
		return new IndexQueryEncounterImpl<Query<T,?,?>, T>(tx, query, id -> this.getInternal(tx, id));
	}

	@Override
	public <R, FR extends FetchResult<R>> Mono<FR> fetch(
		Query<T, R, FR> query
	)
	{
		IndexEngineController<?, ?> controller = queryControllers.get(query.getIndex());
		if(controller == null)
		{
			throw new StorageException("Unknown query engine `" + query.getIndex() + "`");
		}

		return transactionSupport.monoWithExchange(tx ->
			controller.fetch(createQueryEncounter(tx, query)),
			mainDataStorage, primary, controller
		);
	}

	@Override
	public <R> Flux<R> stream(Query<T, R, ?> query)
	{
		IndexEngineController<?, ?> controller = queryControllers.get(query.getIndex());
		if(controller == null)
		{
			throw new StorageException("Unknown query engine `" + query.getIndex() + "`");
		}

		return transactionSupport.fluxWithExchange(tx ->
			controller.stream(createQueryEncounter(tx, query)),
			mainDataStorage, primary, controller
		);
	}

	public Iterator<LongObjectPair<T>> createIterator(long minIdExclusive, long maxIdInclusive)
	{
		long first = primary.nextAfter(minIdExclusive);
		if(first == 0)
		{
			return Collections.emptyIterator();
		}

		return new Iterator<LongObjectPair<T>>()
		{
			private long current = first;

			@Override
			public boolean hasNext()
			{
				return current != 0;
			}

			@Override
			public LongObjectPair<T> next()
			{
				if(current == 0)
				{
					throw new NoSuchElementException();
				}

				try
				{
					long toFetch = current;
					current = primary.nextAfter(current);
					if(current > maxIdInclusive)
					{
						current = 0;
					}

					try(InputStream in = mainDataStorage.get(null, toFetch))
					{
						return PrimitiveTuples.pair(toFetch, codec.decode(in));
					}
				}
				catch(IOException e)
				{
					throw new StorageException("Unable to get next item; " + e.getMessage(), e);
				}
			}
		};
	}

	@Override
	public Flux<T> stream()
	{
		return Flux.fromIterable(() -> {
			long first = primary.first();
			if(first == 0)
			{
				return Collections.emptyIterator();
			}

			return new Iterator<T>()
			{
				private long current = first;

				@Override
				public boolean hasNext()
				{
					return current != 0;
				}

				@Override
				public T next()
				{
					if(current == 0)
					{
						throw new NoSuchElementException();
					}

					try
					{
						long toFetch = current;
						current = primary.nextAfter(current);
						try(InputStream in = mainDataStorage.get(null, toFetch))
						{
							return codec.decode(in);
						}
					}
					catch(IOException e)
					{
						throw new StorageException("Unable to get next item; " + e.getMessage(), e);
					}
				}
			};
		});
	}

	/**
	 * Store an entry for this entity.
	 *
	 * @param id
	 * @param bytes
	 * @throws IOException
	 */
	public void directStore(Object id, InputStream in)
		throws IOException
	{
		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Direct store of " + id);
		}

		long previousInternalId = primary.get(null, id);

		// Store the new data and associate it with the primary index
		long internalId = mainDataStorage.store(in::transferTo);
		primary.store(id, internalId);

		// TODO: Replacement for indexes?
		if(previousInternalId != 0)
		{
			// Remove the old data
			mainDataStorage.delete(previousInternalId);

			queryControllers.each(e -> e.delete(previousInternalId));
		}

		stores.increment();
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
		long internalId = primary.get(null, id);

		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Direct delete of " + id + " mapped to internal id " + internalId);
		}

		if(internalId == 0) return;

		queryControllers.each(e -> e.delete(internalId));

		mainDataStorage.delete(internalId);
		primary.remove(id);

		deletes.increment();
	}

	public void directIndex(String index, Object id, InputStream data)
		throws IOException
	{
		long internalId = primary.get(null, id);

		IndexEngineController<T, ?> controller = queryControllers.get(index);
		if(controller != null)
		{
			controller.store(internalId, data);
		}
	}
}
