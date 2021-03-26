package se.l4.silo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.index.Query;

/**
 * Base for entities in {@link Silo}.
 */
public interface Entity<ID, T>
{
	/**
	 * Get the name of this entity.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get stored data using an identifier.
	 *
	 * @param id
	 * @return
	 */
	Mono<T> get(ID id);

	/**
	 * Check if data with the given identifier exists.
	 *
	 * @param id
	 * @return
	 */
	Mono<Boolean> contains(ID id);

	/**
	 * Store data in the entity.
	 *
	 * @param object
	 * @return
	 */
	Mono<StoreResult<ID, T>> store(T object);

	/**
	 * Delete data associated with the given identifier.
	 *
	 * @param id
	 * @return
	 */
	Mono<DeleteResult<ID, T>> delete(ID id);

	/**
	 * Fetch data from this entity using the given query.
	 *
	 * @param query
	 * @return
	 */
	<R, FR extends FetchResult<R>> Mono<FR> fetch(Query<T, R, FR> query);

	/**
	 * Stream the results of a query.
	 *
	 * @param <R>
	 * @param query
	 * @return
	 */
	<R> Flux<R> stream(Query<T, R, ?> query);
}
