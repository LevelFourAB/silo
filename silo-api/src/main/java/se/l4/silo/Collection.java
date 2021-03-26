package se.l4.silo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.index.Query;

/**
 * Collection of stored objects in {@link Silo}.
 */
public interface Collection<ID, T>
{
	/**
	 * Get the name of this collection.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get stored object using an identifier.
	 *
	 * @param id
	 * @return
	 */
	Mono<T> get(ID id);

	/**
	 * Check if an object with the given identifier exists.
	 *
	 * @param id
	 * @return
	 */
	Mono<Boolean> contains(ID id);

	/**
	 * Store an object in this collection. If the id of the object already
	 * exists it will be replaced, if the id does not exist a new object will
	 * be stored.
	 *
	 * @param object
	 * @return
	 */
	Mono<StoreResult<ID, T>> store(T object);

	/**
	 * Delete object with the given identifier.
	 *
	 * @param id
	 * @return
	 */
	Mono<DeleteResult<ID, T>> delete(ID id);

	/**
	 * Fetch objects using the given query.
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
