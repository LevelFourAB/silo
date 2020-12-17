package se.l4.silo;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Transaction in {@link Silo}. See {@link Silo} for details on the
 * transaction semantics in use.
 */
public interface Transaction
{
	/**
	 * Rollback any changes made.
	 *
	 * @return
	 *   {@link Mono} that will roll back any changes done via this transaction
	 */
	Mono<Void> rollback();

	/**
	 * Commit any changes made.
	 *
	 * @return
	 *   {@link Mono} that will commit any changes done via this transaction
	 */
	Mono<Void> commit();

	/**
	 * Turn the given {@link Mono} into a transactional one that will execute
	 * within this transaction.
	 *
	 * @param <V>
	 * @param mono
	 * @return
	 */
	<V> Mono<V> wrap(Mono<V> mono);

	/**
	 * Turn the given {@link Flux} into a transactional one that will execute
	 * within this transaction.
	 *
	 * @param <V>
	 * @param flux
	 * @return
	 */
	<V> Flux<V> wrap(Flux<V> flux);

	/**
	 * Execute the given function within this transaction.
	 *
	 * <pre>
	 * transaction.execute(tx -> {
	 *   // This will run within this transaction
	 *   return entity.store(new TestData());
	 * });
	 * </pre>
	 *
	 * @param <V>
	 * @param scopeFunction
	 * @return
	 */
	<V> Flux<V> execute(Function<Transaction, Publisher<V>> scopeFunction);
}
