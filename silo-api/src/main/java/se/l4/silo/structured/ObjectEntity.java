package se.l4.silo.structured;

import java.util.Optional;

import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;

/**
 * Entity for storing and retrieving objects.
 *
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface ObjectEntity<T>
	extends Entity
{
	/**
	 * Get an object.
	 *
	 * @param id
	 * @return
	 */
	Optional<T> get(Object id);

	/**
	 * Delete the given object.
	 *
	 * @param id
	 */
	void deleteViaId(Object id);

	/**
	 * Delete the given object.
	 *
	 * @param object
	 *   the object to delete
	 */
	void delete(T object);

	/**
	 * Store an object.
	 *
	 * @param data
	 */
	void store(T data);

	/**
	 * Query the the query engine.
	 *
	 * @param index
	 * @return
	 */
	<RT, Q extends Query<?>> Q query(String engine, QueryType<T, RT, Q> type);

	/**
	 * Stream all of the objects stored in this entity.
	 *
	 * @return
	 */
	FetchResult<T> stream();
}
