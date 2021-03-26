package se.l4.silo.engine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Collection;
import se.l4.silo.engine.index.LocalIndex;

/**
 * Extension to {@link Collection} to represent extra things available on
 * collections that are available locally.
 */
public interface LocalCollection<ID, T>
	extends Collection<ID, T>
{
	/**
	 * Get the number of reads that have occurred for this collection during the
	 * current runtime.
	 *
	 * @return
	 */
	long getReads();

	/**
	 * Get the number of stores that have occurred for this collection during the
	 * current runtime.
	 *
	 * @return
	 */
	long getStores();

	/**
	 * Get the number of deletes that have occurred for this collection during the
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
	 * Get a {@link Flux} with all of the indexes in this collection.
	 *
	 * @return
	 */
	Flux<LocalIndex> indexes();
}
