package se.l4.silo.engine;

/**
 * Encounter with a query, contains information that can be used by a
 * {@link QueryEngine} to return results.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryEncounter<T>
{
	/**
	 * Get the data that describes the query.
	 * 
	 * @return
	 */
	T getData();
	
	/**
	 * Load the given object.
	 * 
	 * @param id
	 * @return
	 */
	Object load(long id);

	/**
	 * Indicate that the given data should be returned as a result for this
	 * query.
	 * 
	 * @param id
	 */
	void receive(long id);
	
	/**
	 * Set the metadata about offset, limit and total number of hits found
	 * for this query.
	 * 
	 * @param offset
	 * @param limit
	 * @param totalHits
	 */
	void setMetadata(int offset, int limit, int totalHits);
}
