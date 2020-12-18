package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.IndexDefinition;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.internal.mvstore.SharedStorages;
import se.l4.silo.engine.internal.query.QueryEncounterImpl;
import se.l4.silo.engine.internal.query.QueryEngineCreationEncounterImpl;
import se.l4.silo.engine.internal.query.QueryEngineUpdater;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.query.Query;
import se.l4.ylem.io.Bytes;

/**
 * Implementation of {@link Storage}.
 */
public class StorageImpl<T>
	implements Storage<T>, Closeable
{
	private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);
	private final TransactionSupport transactionSupport;
	private final DataStorage storage;

	private final String name;
	private final EntityCodec<T> codec;

	private final PrimaryIndex primary;
	private final MapIterable<String, QueryEngine<T, ?>> queryEngines;
	private final QueryEngineUpdater<T> queryEngineUpdater;

	private long previousForIndex;

	public StorageImpl(
		StorageEngine engine,
		SharedStorages storages,
		ScheduledExecutorService executor,
		TransactionSupport transactionSupport,

		MVStoreManager store,
		MVStoreManager stateStore,

		Path dataDir,
		DataStorage storage,

		String name,
		EntityCodec<T> codec,

		PrimaryIndex primary,
		RichIterable<IndexDefinition<?>> indexes
	)
	{
		this.name = name;
		this.transactionSupport = transactionSupport;
		this.storage = storage;
		this.primary = primary;

		this.codec = codec;

		this.queryEngines = (ImmutableMap) indexes.toMap(IndexDefinition::getName, def -> {
			String key = def.getName();
			return def.create(new QueryEngineCreationEncounterImpl(
				storages,
				executor,
				dataDir,
				key,
				name + "-" + key
			));
		}).toImmutable();

		this.queryEngineUpdater = new QueryEngineUpdater<>(
			engine,

			storage,
			store,
			stateStore,
			this,
			executor,
			name,
			this.queryEngines
		);
	}

	@Override
	public void close()
		throws IOException
	{
		// Close all of our query engines
		for(QueryEngine<?, ?> engine : this.queryEngines)
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
				for(QueryEngine<T, ?> engine : queryEngines)
				{
					tx.index(name, engine.getName(), id, out0 -> {
						try(ExtendedDataOutputStream out = new ExtendedDataOutputStream(out0))
						{
							engine.generate(instance, out);
						}
					});
				}

				return new StoreResultImpl<>(id);
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
		});
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
		});
	}

	public T getInternal(TransactionExchange exchange, long id)
	{
		try
		{
			Bytes data = storage.get(exchange, id);
			try(InputStream in = data.asInputStream())
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

	private QueryEncounterImpl createQueryEncounter(Query query)
	{
		return new QueryEncounterImpl<Query<T,?,?>, T>(query, id -> this.getInternal(null, id));
	}

	@Override
	public <R, FR extends FetchResult<R>> Mono<FR> fetch(
		Query<T, R, FR> query
	)
	{
		QueryEngine<?, ?> qe = queryEngines.get(query.getIndex());
		if(qe == null)
		{
			throw new StorageException("Unknown query engine `" + query.getIndex() + "`");
		}

		return qe.fetch(createQueryEncounter(query));
	}

	@Override
	public <R> Flux<R> stream(Query<T, R, ?> query)
	{
		QueryEngine<?, ?> qe = queryEngines.get(query.getIndex());
		if(qe == null)
		{
			throw new StorageException("Unknown query engine `" + query.getIndex() + "`");
		}

		return qe.stream(createQueryEncounter(query));
	}

	public Iterator<LongObjectPair<T>> iterator()
	{
		long first = primary.first();
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
					Bytes data = storage.get(null, toFetch);
					try(InputStream in = data.asInputStream())
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
						Bytes data = storage.get(null, toFetch);
						try(InputStream in = data.asInputStream())
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

		this.previousForIndex = primary.latest();
		long previousInternalId = primary.get(null, id);

		// Store the new data and associate it with the primary index
		long internalId = storage.store(Bytes.capture(in));
		primary.store(id, internalId);

		// TODO: Replacement for indexes?
		if(previousInternalId != 0)
		{
			// Remove the old data
			storage.delete(previousInternalId);
			queryEngineUpdater.delete(previousInternalId);
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
		long internalId = primary.get(null, id);

		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Direct delete of " + id + " mapped to internal id " + internalId);
		}

		if(internalId == 0) return;

		queryEngineUpdater.delete(internalId);

		storage.delete(internalId);
		primary.remove(id);
	}

	public void directIndex(String index, Object id, InputStream data)
		throws IOException
	{
		long internalId = primary.get(null, id);
		queryEngineUpdater.store(this.previousForIndex, internalId, index, data);
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
