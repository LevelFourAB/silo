package se.l4.silo.results;

import se.l4.silo.FetchResult;

public interface TotalAwareResult<T>
	extends FetchResult<T>
{
	/**
	 * Get the total number of items available. This includes items that have
	 * not been fetched.
	 *
	 * @return
	 */
	long getTotal();
}
