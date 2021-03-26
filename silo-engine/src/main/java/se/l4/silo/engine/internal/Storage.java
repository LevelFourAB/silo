package se.l4.silo.engine.internal;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Collection;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.index.IndexDef;
import se.l4.silo.engine.index.LocalIndex;
import se.l4.silo.index.Query;

/**
 * Storage that can be used by {@link Collection collections}.
 */
public interface Storage<T>
{
	/**
	 * Get the number of reads that have occurred in this storage.
	 *
	 * @return
	 */
	long getReads();

	/**
	 * Get the number of stores that have occurred in this storage.
	 *
	 * @return
	 */
	long getStores();

	/**
	 * Get the number of deletes that have occurred in this storage.
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
	Mono<LocalIndex> getIndex(String name);

	/**
	 * Get a {@link Flux} with all of the indexes in this collection.
	 *
	 * @return
	 */
	Flux<LocalIndex> indexes();

	/**
	 * Store some data in this storage.
	 *
	 * @param id
	 * @param data
	 * @return
	 */
	<ID> Mono<StoreResult<ID, T>> store(ID id, T data);

	/**
	 * Get some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	Mono<T> get(Object id);

	/**
	 * Check if some data exists in this storage.
	 *
	 * @param id
	 * @return
	 */
	Mono<Boolean> contains(Object id);

	/**
	 * Delete some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	<ID> Mono<DeleteResult<ID, T>> delete(ID id);

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
		Builder<T> addIndex(IndexDef<T> index);

		/**
		 * Add all of the given indexes to this storage.
		 *
		 * @param indexes
		 * @return
		 *   new instance
		 */
		Builder<T> addIndexes(Iterable<IndexDef<T>> indexes);

		/**
		 * Build and return the storage.
		 *
		 * @return
		 */
		Storage<T> build();
	}
}
