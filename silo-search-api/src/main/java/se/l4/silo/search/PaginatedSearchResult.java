package se.l4.silo.search;

import se.l4.silo.results.LimitedFetchResult;

/**
 * {@link SearchResult} that is paginated.
 */
public interface PaginatedSearchResult<T>
	extends SearchResult<T>, LimitedFetchResult<SearchHit<T>>
{

}
