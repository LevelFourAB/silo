package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.QueryResult;
import se.l4.silo.StoreResult;

/**
 * Storage that can be used by {@link Entity entities}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Storage
{
	/**
	 * Store some data in this storage.
	 * 
	 * @param id
	 * @param bytes
	 * @return
	 */
	StoreResult store(Object id, Bytes bytes);
	
	/**
	 * Get some data in this storage.
	 * 
	 * @param id
	 * @return
	 */
	Bytes get(Object id);

	/**
	 * Delete some data in this storage.
	 * 
	 * @param id
	 * @return
	 */
	DeleteResult delete(Object id);
	
	/**
	 * Invoke a query engine by passing it a pre-built query.
	 * 
	 * @param engine
	 * @param query
	 * @return 
	 */
	<R> FetchResult<QueryResult<R>> query(String engine, Object query, Function<Bytes, R> dataLoader);
}
