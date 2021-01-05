package se.l4.silo.index.search;

import se.l4.silo.results.SizeAwareResult;
import se.l4.silo.results.TotalAwareResult;

public interface SearchResult<T>
	extends SizeAwareResult<SearchHit<T>>, TotalAwareResult<SearchHit<T>>
{
	/**
	 * Get information about facets returned in this result.
	 *
	 * @return
	 */
	Facets facets();

	/**
	 * Get if the {@link #getTotal()} is an estimated number.
	 *
	 * @return
	 */
	boolean isEstimatedTotal();
}
