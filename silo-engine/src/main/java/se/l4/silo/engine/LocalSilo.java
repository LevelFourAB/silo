package se.l4.silo.engine;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

import se.l4.silo.ResourceHandle;
import se.l4.silo.Silo;
import se.l4.silo.Transaction;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.internal.StorageEngine;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.engine.internal.tx.TransactionImpl;
import se.l4.silo.engine.internal.tx.WrappedTransaction;
import se.l4.silo.engine.log.LogBuilder;

/**
 * {@link Silo} instance that stores data locally. You can use this to embed
 * the storage together with a server.
 * 
 * @author Andreas Holstenson
 *
 */
public class LocalSilo
	implements Silo
{
	private final StorageEngine storageEngine;
	private final Supplier<TransactionExchange> exchanges;
	private final ThreadLocal<TransactionImpl> transactions;

	public LocalSilo(LogBuilder logBuilder, Path storage, EngineConfig config)
	{
		Objects.requireNonNull(logBuilder, "logBuilder is required");
		Objects.requireNonNull(storage, "storage path is required");
		Objects.requireNonNull(config, "configuration is required");
		
		storageEngine = new StorageEngine(logBuilder, storage, config);
		
		exchanges = this::getExchange;
		transactions = new ThreadLocal<>();
	}

	@Override
	public void start()
		throws Exception
	{
		
	}

	@Override
	public void stop()
		throws Exception
	{
		storageEngine.close();
	}
	
	private TransactionExchange getExchange()
	{
		TransactionImpl tx = transactions.get();
		if(tx != null)
		{
			return tx.getExchange();
		}

		// No active transaction, create a new transaction
		return new TransactionImpl(storageEngine.getTransactionLog(), (i) -> {});
	}

	@Override
	public <T> T create(Class<T> siloInterface)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public BinaryEntity binary(String entityName)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transaction newTransaction()
	{
		TransactionImpl tx = transactions.get();
		if(tx != null)
		{
			return new WrappedTransaction(tx);
		}
		
		tx = new TransactionImpl(storageEngine.getTransactionLog(), this::deactivateTransaction);
		transactions.set(tx);
		return tx;
	}
	
	private void deactivateTransaction(Transaction tx)
	{
		if(transactions.get() == tx)
		{
			transactions.remove();
		}
	}

	@Override
	public ResourceHandle acquireResourceHandle()
	{
		return new ResourceHandle()
		{
			@Override
			public void close()
			{
				// Local implementations of Silo have nothing to close
			}
		};
	}
	
}
