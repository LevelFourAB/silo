package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.collections.api.list.ListIterable;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Entity;
import se.l4.silo.EntityRef;
import se.l4.silo.Silo;
import se.l4.silo.StorageException;
import se.l4.silo.Transaction;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;

/**
 * {@link Silo} instance that stores data locally. You can use this to embed
 * the storage together with a server.
 *
 */
public class LocalSiloImpl
	implements LocalSilo
{
	private final StorageEngine storageEngine;
	private final TransactionSupport transactionSupport;

	@SuppressWarnings("rawtypes")
	LocalSiloImpl(
		Vibe vibe,
		LogBuilder logBuilder,
		Path storage,
		EngineConfig config,
		ListIterable<EntityDefinition> entities
	)
	{
		storageEngine = new StorageEngine(vibe, logBuilder, storage, config, entities);

		transactionSupport = storageEngine.getTransactionSupport();
	}

	@Override
	public void close()
	{
		try
		{
			storageEngine.close();
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to close; " + e.getMessage(), e);
		}
	}


	@Override
	public boolean hasEntity(String entityName)
	{
		return storageEngine.getEntity(entityName) != null;
	}

	@Override
	public <ID, T> Entity<ID, T> entity(EntityRef<ID, T> ref)
	{
		Objects.requireNonNull(ref);

		Entity<?, ?> entity = storageEngine.getEntity(ref.getName());
		if(entity == null)
		{
			throw new StorageException("The entity `" + ref.getName() + "` does not exist");
		}

		// TODO: Validate the type before returning
		return (Entity<ID, T>) entity;
	}

	@Override
	public Mono<Transaction> newTransaction()
	{
		return transactionSupport.newTransaction();
	}

	@Override
	public <V> Flux<V> transactional(Flux<V> flux)
	{
		return transactionSupport.transactional(flux);
	}

	@Override
	public <V> Mono<V> transactional(Mono<V> mono)
	{
		return transactionSupport.transactional(mono);
	}

	@Override
	public Mono<Void> inTransaction(Runnable runnable)
	{
		return transactionSupport.inTransaction(runnable);
	}

	@Override
	public <T> Mono<T> inTransaction(Supplier<T> supplier)
	{
		return transactionSupport.inTransaction(supplier);
	}

	@Override
	public <V> Flux<V> withTransaction(
		Function<Transaction, Publisher<V>> scopeFunction
	)
	{
		return transactionSupport.withTransaction(scopeFunction);
	}

	/**
	 * Create a snapshot of this instance.
	 *
	 * @return
	 */
	@Override
	public Snapshot createSnapshot()
	{
		return storageEngine.createSnapshot();
	}

	@Override
	public void installSnapshot(Snapshot snapshot)
		throws IOException
	{
		storageEngine.installSnapshot(snapshot);
	}

	public void compact(long time, TimeUnit unit)
	{
		storageEngine.compact(time, unit);
	}
}
