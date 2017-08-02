package se.l4.silo.search;

import java.util.Locale;
import java.util.function.Supplier;

import se.l4.silo.query.LimitableQuery;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;
import se.l4.silo.search.facet.FacetQueryType;

/**
 * Query for a search index. Support several differents ways to query
 * an index, including {@code and} and {@code or} operators.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
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
	 * Set the locale this query is being performed from. This is used for
	 * query parsing in the correct language.
	 * 
	 * @param locale
	 * @return
	 */
	SearchIndexQuery<T> fromLocale(Locale locale);
	
	/**
	 * Indicate that this query should wait for the absolute freshest index
	 * data. By default a search index lags after as reopening the index has
	 * a cost associated with it. Use this method to request that this query
	 * is run against the absolute stored data. This is useful in the
	 * scenario where a user stores something and you need to query for it
	 * directly afterwards.
	 * 
	 * @return
	 */
	SearchIndexQuery<T> waitForLatest();
	
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
	 * Request that this pipeline should sort its result with some extra
	 * parameters.
	 * 
	 * @param field
	 * @param ascending
	 * @param scoring
	 * @return
	 */
	<C extends ScoringQueryBuilder<SearchIndexQuery<T>>> C addSort(String field, boolean ascending, Supplier<C> scoring);
	
	/**
	 * Request that we should use custom scoring.
	 * 
	 * @param scoring
	 * @return
	 */
	<C extends ScoringQueryBuilder<SearchIndexQuery<T>>> C setScoring(Supplier<C> scoring);
}
