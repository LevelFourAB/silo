package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.eclipse.collections.api.list.ListIterable;

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
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.engine.internal.tx.TransactionExchangeImpl;
import se.l4.silo.engine.internal.tx.WrappedTransaction;
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
	private final TransactionSupport tx;
	private final StorageEngine storageEngine;
	private final ThreadLocal<TransactionExchangeImpl> transactions;

	@SuppressWarnings("rawtypes")
	LocalSiloImpl(
		Vibe vibe,
		LogBuilder logBuilder,
		Path storage,
		EngineConfig config,
		ListIterable<EntityDefinition> entities
	)
	{
		tx = createTransactionSupport();
		transactions = new ThreadLocal<>();

		storageEngine = new StorageEngine(vibe, tx, logBuilder, storage, config, entities);
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

	private TransactionSupport createTransactionSupport()
	{
		return new TransactionSupport()
		{
			@Override
			public TransactionExchange getExchange()
			{
				TransactionExchangeImpl tx = transactions.get();
				if(tx != null)
				{
					return tx.getExchange();
				}

				// No active transaction, create a new transaction
				return new TransactionExchangeImpl(storageEngine.getTransactionLog(), (i) -> {});
			}

			@Override
			public Transaction newTransaction()
			{
				TransactionExchangeImpl tx = transactions.get();
				if(tx != null)
				{
					return new WrappedTransaction(tx.getTransaction());
				}

				tx = new TransactionExchangeImpl(storageEngine.getTransactionLog(), this::deactivateTransaction);
				transactions.set(tx);
				return tx.getTransaction();
			}

			private void deactivateTransaction(TransactionExchangeImpl tx)
			{
				if(transactions.get() == tx)
				{
					transactions.remove();
				}
			}
		};
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
		return Mono.fromSupplier(tx::newTransaction);
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
