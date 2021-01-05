package se.l4.silo.engine.index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Information about a local index.
 */
public interface LocalIndex
{
	/**
	 * Get the name of this index.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the number of times this index has been queried during the current
	 * runtime.
	 *
	 * @return
	 */
	long getQueryCount();

	/**
	 * Get if the index is considered up to date.
	 *
	 * @return
	 *   {@code true} if the index is up to date, {@code false} otherwise
	 */
	boolean isUpToDate();

	/**
	 * Get a {@link Mono} that will resolve when this index is considered up
	 * to date.
	 *
	 * @return
	 */
	Mono<Void> whenUpToDate();

	/**
	 * Get if this index can currently be queried.
	 *
	 * @return
	 */
	boolean isQueryable();

	/**
	 * Get a {@link Mono} that will resolve when this index is ready to be
	 * queried.
	 *
	 * @return
	 */
	Mono<Void> whenQueryable();

	/**
	 * Listen to events on this index.
	 *
	 * @return
	 */
	Flux<IndexEvent> events();
}
