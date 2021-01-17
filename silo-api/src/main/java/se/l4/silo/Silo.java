package se.l4.silo;

import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
 */
public interface Silo
{
	/**
	 * Check if the given entity is available.
	 *
	 * @param entityName
	 * @return
	 */
	boolean hasEntity(String entityName);

	/**
	 * Get an entity.
	 *
	 * @param entityName
	 * @param ref
	 */
	<ID, T> Entity<ID, T> entity(EntityRef<ID, T> ref);

	/**
	 * Get an entity.
	 *
	 * @param entityName
	 * @param type
	 */
	default <ID, T> Entity<ID, T> entity(String name, Class<ID> idType, Class<T> objectType)
	{
		return entity(EntityRef.create(name, idType, objectType));
	}

	/**
	 * Access support for transactions.
	 *
	 * @return
	 */
	Transactions transactions();
}
