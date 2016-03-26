package se.l4.silo.search;

import se.l4.silo.query.LimitableQuery;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;

public interface SearchIndexQuery<T>
	extends Query<SearchResult<T>>,
		LimitableQuery<SearchIndexQuery<T>>,
		QueryWithSubqueries<SearchIndexQuery<T>, SearchIndexQuery<T>>
{
	static <T> QueryType<T, SearchHit<T>, SearchIndexQuery<T>> type()
	{
		return new SearchIndexQueryType<>();
	}
	
	/**
	 * Define a facet that we should use.
	 * 
	 * @param id
	 * @return
	 */
	FacetQueryBuilder withFacet(String id);
	
	/**
	 * Request that this pipeline should sort its results.
	 *  
	 * @param sort
	 * @param sortAscending
	 * @return
	 */
	SearchIndexQuery<T> addSort(String sort, boolean sortAscending);
	
	/**
	 * Request that we should use custom scoring.
	 * 
	 * @param scoring
	 * @return
	 */
	SearchIndexQuery<T> setScoring(String scoring);
}
