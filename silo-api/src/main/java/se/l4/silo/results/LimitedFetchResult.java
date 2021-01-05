package se.l4.silo.results;

import se.l4.silo.FetchResult;
import se.l4.silo.index.LimitableQuery;

/**
 * Result of a fetch that also includes information about the offset and limit
 * of items. Commonly returned when a {@link LimitableQuery} is used.
 */
public interface LimitedFetchResult<T>
	extends FetchResult<T>
{
	/**
	 * Get the offset these results start at.
	 *
	 * @return
	 */
	long getOffset();

	/**
	 * Get the limit used for these results.
	 *
	 * @return
	 */
	long getLimit();
}
