package se.l4.silo.search;

import java.util.function.Supplier;

import se.l4.silo.query.LimitableQuery;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;
import se.l4.silo.search.facet.FacetQueryType;

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
	<C extends FacetQueryBuilder<SearchIndexQuery<T>>> C withFacet(String id, FacetQueryType<SearchIndexQuery<T>, C> type);
	
	/**
	 * Define a facet that we should use.
	 * 
	 * @param id
	 * @return
	 */
	<C extends FacetQueryBuilder<SearchIndexQuery<T>>> C withFacet(String id, Supplier<C> facetType);
	
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