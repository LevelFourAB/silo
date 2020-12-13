package se.l4.silo.results;

import se.l4.silo.FetchResult;

/**
 * Result of a fetch that also includes information about the total number
 * of items fetched.
 */
public interface SizeAwareResult<T>
	extends FetchResult<T>
{
	/**
	 * Get the number of items that were fetched.
	 *
	 * @return
	 */
	long getSize();
}
