package se.l4.silo;

import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Transaction support, allows for various ways to create and work with
 * transactions.
 */
public interface Transactions
{
	/**
	 * Create a new transaction that can be used for full control over a
	 * transaction.
	 *
	 * @return
	 */
	Mono<Transaction> newTransaction();

	/**
	 * Wrap the given {@link Mono} making it transactional.
	 *
	 * @param <V>
	 * @param mono
	 * @return
	 */
	<V> Mono<V> transactional(Mono<V> mono);

	/**
	 * Wrap the given {@link Flux} making it transactional.
	 *
	 * @param <V>
	 * @param flux
	 * @return
	 */
	<V> Flux<V> transactional(Flux<V> flux);

	/**
	 * Perform an operation within a transaction.
	 *
	 * <pre>
	 * transactions.withTransaction(tx -> {
	 *   // This runs within a transaction
	 *   return collection.store(new TestData());
	 * })
	 *   // Everything else is outside the transaction
	 *   .map(result -> ...);
	 * </pre>
	 *
	 * @param <V>
	 * @param scopeFunction
	 * @return
	 */
	<V> Flux<V> withTransaction(Function<Transaction, Publisher<V>> scopeFunction);

	/**
	 * Run the given {@link Supplier} in a transaction.
	 *
	 *
	 * @param supplier
	 * @return
	 */
	<T> Mono<T> inTransaction(Supplier<T> supplier);

	/**
	 * Run the given {@link Runnable} in a transaction.
	 *
	 * @param runnable
	 * @return
	 */
	Mono<Void> inTransaction(Runnable runnable);
}
