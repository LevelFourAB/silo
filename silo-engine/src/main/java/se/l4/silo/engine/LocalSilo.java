package se.l4.silo.engine;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import se.l4.silo.ResourceHandle;
import se.l4.silo.Silo;
import se.l4.silo.Transaction;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.internal.BinaryEntityOverLog;
import se.l4.silo.engine.internal.EntityChangeListener;
import se.l4.silo.engine.internal.StorageEngine;
import se.l4.silo.engine.internal.StorageEntity;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.engine.internal.tx.TransactionImpl;
import se.l4.silo.engine.internal.tx.WrappedTransaction;
import se.l4.silo.engine.log.DirectApplyLog;
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
	private final ConcurrentHashMap<String, Object> entities;
	
	private final StorageEngine storageEngine;
	private final Supplier<TransactionExchange> exchanges;
	private final ThreadLocal<TransactionImpl> transactions;

	public LocalSilo(LogBuilder logBuilder, Path storage, EngineConfig config)
	{
		Objects.requireNonNull(logBuilder, "logBuilder is required");
		Objects.requireNonNull(storage, "storage path is required");
		Objects.requireNonNull(config, "configuration is required");
		
		entities = new ConcurrentHashMap<>();
		
		exchanges = this::getExchange;
		transactions = new ThreadLocal<>();
		
		storageEngine = new StorageEngine(logBuilder, storage, config, createEntityListener());
	}
	
	public static Silo open(Path path, EngineConfig config)
	{
		return new LocalSilo(DirectApplyLog.builder(), path, config);
	}
	
	public static Silo open(File path, EngineConfig config)
	{
		return open(path.toPath(), config);
	}

	private EntityChangeListener createEntityListener()
	{
		return new EntityChangeListener()
		{
			@Override
			public void removeEntity(String name)
			{
				entities.remove(name);
			}
			
			@Override
			public void newBinaryEntity(String name, StorageEntity storageEntity)
			{
				BinaryEntityOverLog entity = new BinaryEntityOverLog(name, exchanges, storageEntity);
				entities.put(name, entity);
			}
		};
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
	
	@SuppressWarnings("unchecked")
	private <T> T entity(String entityName)
	{
		return (T) entities.get(entityName);
	}

	@Override
	public BinaryEntity binary(String entityName)
	{
		return entity(entityName);
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
