package se.l4.silo.engine.internal;

import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Transaction;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;

/**
 * Transaction support as seen internally in the storage engine.
 */
public interface TransactionSupport
{
	/**
	 * Register a value that should be provided in instances of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param value
	 */
	void registerValue(TransactionValue<?> value);

	/**
	 * Create a {@link Transaction} for manual control over when it is
	 * committed.
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
	 * silo.withTransaction(tx -> {
	 *   // This runs within a transaction
	 *   return entity.store(new TestData());
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

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Mono<V> withExchange(Function<WriteableTransactionExchange, V> func);

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Mono<V> monoWithExchange(Function<WriteableTransactionExchange, Mono<V>> func);

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Flux<V> fluxWithExchange(Function<WriteableTransactionExchange, Flux<V>> func);
}
