package se.l4.silo.engine.internal.tx;

import reactor.core.publisher.Mono;

/**
 * Support for waiting for transactions to be applied.
 */
public interface TransactionWaiter
{
	/**
	 * Get a {@link Mono} that will wait for the given transaction.
	 *
	 * @param tx
	 */
	Mono<Void> getWaiter(long tx);
}
