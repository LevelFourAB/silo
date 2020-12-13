package se.l4.silo;

import reactor.core.publisher.Mono;

/**
 * Transaction in {@link Silo}. See {@link Silo} for details on the
 * transaction semantics in use.
 */
public interface Transaction
{
	/**
	 * Rollback any changes made.
	 */
	Mono<Void> rollback();

	/**
	 * Commit any changes made.
	 */
	Mono<Void> commit();
}
