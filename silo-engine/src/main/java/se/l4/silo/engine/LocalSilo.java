package se.l4.silo.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import se.l4.commons.serialization.SerializerCollection;
import se.l4.silo.ResourceHandle;
import se.l4.silo.Silo;
import se.l4.silo.StorageException;
import se.l4.silo.Transaction;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.internal.LocalEngineFactories;
import se.l4.silo.engine.internal.LocalSiloBuilder;
import se.l4.silo.engine.internal.StorageEngine;
import se.l4.silo.engine.internal.TransactionSupport;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.silo.engine.internal.tx.TransactionImpl;
import se.l4.silo.engine.internal.tx.WrappedTransaction;
import se.l4.silo.engine.log.DirectApplyLog;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.silo.structured.StructuredEntity;
import se.l4.vibe.Vibe;

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
	
	private final TransactionSupport tx;
	private final StorageEngine storageEngine;
	private final ThreadLocal<TransactionImpl> transactions;

	public LocalSilo(LocalEngineFactories factories, SerializerCollection serializers, Vibe vibe, LogBuilder logBuilder, Path storage, EngineConfig config)
	{
		Objects.requireNonNull(factories, "factories is required");
		Objects.requireNonNull(serializers, "serializers is required");
		Objects.requireNonNull(logBuilder, "logBuilder is required");
		Objects.requireNonNull(storage, "storage path is required");
		Objects.requireNonNull(config, "configuration is required");
		
		entities = new ConcurrentHashMap<>();
		
		tx = createTransactionSupport();
		transactions = new ThreadLocal<>();
		
		storageEngine = new StorageEngine(factories, serializers, vibe, tx, logBuilder, storage, config);
	}
	
	/**
	 * Start creating a local instance of Silo. The built instance will use a
	 * {@link DirectApplyLog} so any operations will be applied directly by the
	 * calling thread.
	 * 
	 * @param path
	 *   the directory where data will be stored
	 * @return
	 */
	public static SiloBuilder open(Path path)
	{
		return new LocalSiloBuilder(DirectApplyLog.builder(), path);
	}
	
	/**
	 * Start creating a local instance of Silo. See {@link #open(Path)} for
	 * details about how the instance will behave.
	 * 
	 * @param path
	 *   the directory where data will be stored
	 * @return
	 */
	public static SiloBuilder open(File path)
	{
		return open(path.toPath());
	}
	
	/**
	 * Start creating a new instance using a specific log to apply operations.
	 * 
	 * @param logBuilder
	 * @param path
	 * @return
	 */
	public static SiloBuilder open(LogBuilder logBuilder, Path path)
	{
		return new LocalSiloBuilder(logBuilder, path);
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
				TransactionImpl tx = transactions.get();
				if(tx != null)
				{
					return tx.getExchange();
				}

				// No active transaction, create a new transaction
				return new TransactionImpl(storageEngine.getTransactionLog(), (i) -> {});
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
		};
	}

	@Override
	public <T> T create(Class<T> siloInterface)
	{
		throw new UnsupportedOperationException();
	}
	
	private <T> T entity(String entityName)
	{
		return storageEngine.getEntity(entityName);
	}

	@Override
	public BinaryEntity binary(String entityName)
	{
		return entity(entityName);
	}
	
	@Override
	public StructuredEntity structured(String entityName)
	{
		return entity(entityName);
	}
	
	@Override
	public Transaction newTransaction()
	{
		return tx.newTransaction();
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
	
	/**
	 * Create a snapshot of this instance.
	 * 
	 * @return
	 */
	public Snapshot createSnapshot()
	{
		return storageEngine.createSnapshot();
	}
	
	public void installSnapshot(Snapshot snapshot)
		throws IOException
	{
		storageEngine.installSnapshot(snapshot);
	}
}
