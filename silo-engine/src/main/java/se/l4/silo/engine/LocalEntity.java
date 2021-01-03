package se.l4.silo.engine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Entity;

/**
 * Extension to {@link Entity} to represent extra things available on entities
 * available locally.
 */
public interface LocalEntity<ID, T>
	extends Entity<ID, T>
{
	/**
	 * Get the number of reads that have occurred for this entity during the
	 * current runtime.
	 *
	 * @return
	 */
	long getReads();

	/**
	 * Get the number of stores that have occurred for this entity during the
	 * current runtime.
	 *
	 * @return
	 */
	long getStores();

	/**
	 * Get the number of deletes that have occurred for this entity during the
	 * current runtime.
	 *
	 * @return
	 */
	long getDeletes();

	/**
	 * Get information about a specific index.
	 *
	 * @param name
	 *   the name of the index
	 * @return
	 *   {@link Mono} that will resolve to the index
	 */
	Mono<LocalIndex> index(String name);

	/**
	 * Get a {@link Flux} with all of the indexes in this entity.
	 *
	 * @return
	 */
	Flux<LocalIndex> indexes();
}