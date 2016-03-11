package se.l4.silo.structured;

import se.l4.silo.Entity;
import se.l4.silo.Query;
import se.l4.silo.QueryType;

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
	T get(Object id);
	
	/**
	 * Delete the given object.
	 * 
	 * @param id
	 */
	void delete(Object id);
	
	/**
	 * Store an object.
	 * 
	 * @param data
	 */
	void store(Object id, T data);
	
	/**
	 * Query the the query engine.
	 * 
	 * @param index
	 * @return
	 */
	<RT, Q extends Query<RT>> Q query(String engine, QueryType<T, RT, Q> type);
}
