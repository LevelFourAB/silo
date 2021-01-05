package se.l4.silo.index;

import se.l4.silo.FetchResult;

/**
 * Query for a certain index.
 */
public interface Query<T, R, FR extends FetchResult<R>>
{
	/**
	 * Get the index this query runs on.
	 *
	 * @return
	 */
	String getIndex();
}
