package se.l4.silo;

import java.util.function.Supplier;

import com.google.common.base.Throwables;

import se.l4.crayon.services.ManagedService;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.structured.StructuredEntity;

/**
 * Silo storage.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Silo
	extends ManagedService
{
	/**
	 * Create a new instance of the specified interface for usage with the
	 * storage.
	 * 
	 * @param siloInterface
	 * @return
	 */
	<T> T create(Class<T> siloInterface);
	
	/**
	 * Get access to the binary entity with the given name.
	 * 
	 * @param entityName
	 * @return
	 */
	BinaryEntity binary(String entityName);
	
	/**
	 * Get an entity used for storing structured data with the given name.
	 * 
	 * @param entityName
	 * @return
	 */
	StructuredEntity structured(String entityName);
	
	/**
	 * Create a new transaction for the current thread.
	 * 
	 * @return
	 */
	Transaction newTransaction();
	
	/**
	 * Acquire a new resource lock for the current thread.
	 * 
	 * @return
	 */
	ResourceHandle acquireResourceHandle();
	
	/**
	 * Run the given {@link Supplier} in a transaction.
	 * 
	 * @param supplier
	 * @return
	 */
	default <T> T inTransaction(Supplier<T> supplier)
	{
		StorageTransactionException firstException = null;
		for(int i=0, n=5; i<n; i++)
		{
			Transaction tx = newTransaction();
			try
			{
				T result = supplier.get();
				tx.commit();
				return result;
			}
			catch(StorageTransactionException e)
			{
				// TODO: Slight delay before retrying?
				firstException = e;
				tx.rollback();
			}
			catch(Throwable t)
			{
				tx.rollback();
				Throwables.propagateIfPossible(t);
				throw Throwables.propagate(t);
			}
		}
		
		throw firstException;
	}

	/**
	 * Run the given {@link Runnable} in a transaction.
	 * 
	 * @param runnable
	 * @return
	 */
	default void inTransaction(Runnable runnable)
	{
		StorageTransactionException firstException = null;
		for(int i=0, n=5; i<n; i++)
		{
			Transaction tx = newTransaction();
			try
			{
				runnable.run();
				tx.commit();
				return;
			}
			catch(StorageTransactionException e)
			{
				// TODO: Slight delay before retrying?
				firstException = e;
				tx.rollback();
			}
			catch(Throwable t)
			{
				tx.rollback();
				Throwables.propagateIfPossible(t);
				throw Throwables.propagate(t);
			}
		}
		
		throw firstException;
	}
}
