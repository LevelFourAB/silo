package se.l4.silo;

import java.io.Closeable;
import java.util.function.Supplier;

import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.structured.StructuredEntity;

/**
 * Silo storage, provides access to all entities stored, transactions and
 * resource management. An instance of this class contains {@link Entity entities}
 * that can be used to store and retrieve data.
 *
 * <h2>Transactions</h2>
 * <p>
 * Silo provides transaction support. In normal usage every write operation is
 * a tiny transaction, so a store operation will internally be mapped against a
 * transaction. Transactions in Silo are implemented so that they become
 * readable when they are committed, so any changes made in a transaction
 * are not visible either within our outside the transaction.
 *
 * <h3>Using {@link #newTransaction()}</h3>
 * <p>
 * When {@link #newTransaction()} is called a transaction is activated for the
 * current thread. Transactions <strong>must</strong> either be committed or
 * rolled back when they are used.
 *
 * <p>
 * Example:
 * <pre>
 * Transaction tx = silo.newTransaction();
 * try {
 *   // Perform operations as usual here
 *   entity.store("test id", object);
 *
 *   tx.commit();
 * } catch(Throwable t) {
 *   tx.rollback();
 * }
 * </pre>
 *
 * <h3>Using {@link #inTransaction(Runnable)} and {@link #inTransaction(Supplier)}</h3>
 * <p>
 * An alternative way of using transactions is provided via {@link #inTransaction(Runnable)}
 * and {@link #inTransaction(Supplier)}. These will take care of committing
 * and rolling back a transaction.
 *
 * <p>
 * Examples:
 * <pre>
 * silo.inTransaction(() -> {
 *   entity.store("test id", object);
 * });
 * </pre>
 *
 * @author Andreas Holstenson
 *
 */
public interface Silo
	extends Closeable
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
	 * Check if the given entity is available.
	 *
	 * @param entityName
	 * @return
	 */
	boolean hasEntity(String entityName);

	/**
	 * Get an entity of the given type.
	 *
	 * @param entityName
	 * @param type
	 */
	<T extends Entity> T entity(String entityName, Class<T> type);

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
				if(t instanceof RuntimeException)
				{
					throw (RuntimeException) t;
				}

				throw new StorageException("Uncaught error while handling transaction; " + t.getMessage(), t);
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
				if(t instanceof RuntimeException)
				{
					throw (RuntimeException) t;
				}

				throw new StorageException("Uncaught error while handling transaction; " + t.getMessage(), t);
			}
		}

		throw firstException;
	}

	@Override
	void close();
}
