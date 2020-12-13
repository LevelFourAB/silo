package se.l4.silo.engine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;
import se.l4.silo.query.Query;

/**
 * Storage that can be used by {@link Entity entities}.
 */
public interface Storage<T>
{
	/**
	 * Store some data in this storage.
	 *
	 * @param id
	 * @param data
	 * @return
	 */
	Mono<StoreResult<T>> store(Object id, T data);

	/**
	 * Get some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	Mono<T> get(Object id);

	/**
	 * Delete some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	Mono<DeleteResult> delete(Object id);

	/**
	 * Invoke a query engine by passing it a pre-built query.
	 *
	 * @param engine
	 * @param query
	 * @return
	 */
	<R, FR extends FetchResult<R>> Mono<FR> fetch(Query<T, R, FR> query);

	/**
	 * Invoke a query engine by passing it a pre-built query.
	 *
	 * @param engine
	 * @param query
	 * @return
	 */
	<R> Flux<R> stream(Query<T, R, ?> query);

	/**
	 * Stream everything in this storage.
	 *
	 * @return
	 */
	Flux<T> stream();

	interface Builder<T>
	{
		/**
		 * Add an index to this storage.
		 *
		 * @param index
		 * @return
		 *   new instance
		 */
		Builder<T> addIndex(IndexDefinition<T> index);

		/**
		 * Add all of the given indexes to this storage.
		 *
		 * @param indexes
		 * @return
		 *   new instance
		 */
		Builder<T> addIndexes(Iterable<IndexDefinition<T>> indexes);

		/**
		 * Build and return the storage.
		 *
		 * @return
		 */
		Storage<T> build();
	}
}
