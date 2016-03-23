package se.l4.silo.query;

/**
 * Individual result of a query operation. Used by {@link QueryType} to
 * translate results into a suitable representation.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryResult<T>
{
	/**
	 * Get the main data of the result.
	 * 
	 * @return
	 */
	T getData();
	
	/**
	 * Get some metadata from this query result.
	 * 
	 * @param key
	 * @return
	 */
	Object getMetadata(String key);
}
