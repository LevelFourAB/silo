package se.l4.silo.search;

import se.l4.silo.FetchResult;

public interface SearchResult<T>
	extends FetchResult<SearchHit<T>>
{
	/**
	 * Get information about facets returned in this result.
	 *
	 * @return
	 */
	Facets facets();
}
