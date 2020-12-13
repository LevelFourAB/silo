package se.l4.silo.search;

import java.util.Locale;
import java.util.Optional;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.query.FieldSort;
import se.l4.silo.query.FieldSortBuilder;
import se.l4.silo.query.LimitableQuery;
import se.l4.silo.query.Query;
import se.l4.silo.search.internal.SearchIndexQueryImpl;
import se.l4.silo.search.query.QueryBranch;

public interface SearchIndexQuery<T, FR extends SearchResult<T>>
	extends Query<T, SearchHit<T>, FR>
{
	/**
	 * Get the locale used for queries.
	 *
	 * @return
	 */
	Optional<Locale> getLocale();

	/**
	 * Get clauses that should match.
	 *
	 * @return
	 */
	ListIterable<QueryClause> getClauses();

	/**
	 * Get information about sorting of this result.
	 *
	 * @return
	 */
	ListIterable<FieldSort> getSortOrder();

	/**
	 * Get if this query should wait for the latest search data.
	 *
	 * @return
	 */
	boolean isWaitForLatest();

	static <T> Builder<T> create(String name, Class<T> type)
	{
		return SearchIndexQueryImpl.create(name, type);
	}

	interface Limited<T>
		extends SearchIndexQuery<T, PaginatedSearchResult<T>>, LimitableQuery
	{
	}

	/**
	 * Builder for {@link SearchIndexQuery} containing the common functionality
	 * between different types of queries.
	 */
	interface BaseBuilder<Self extends BaseBuilder<Self>>
		extends QueryBranch.Builder<Self>
	{

		/**
		 * Set the locale this query is being performed from. This is used for
		 * query parsing in the correct language.
		 *
		 * @param locale
		 * @return
		 */
		Self withLocale(Locale locale);

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
		default Self waitForLatest()
		{
			return withWaitForLatest(true);
		}

		/**
		 * Set if this query should wait for the latest results to be available
		 * or not.
		 *
		 * @param waitForLatest
		 * @return
		 */
		Self withWaitForLatest(boolean waitForLatest);

		/**
		 * Sort on the given field, specifying ascending or descending order
		 * in a fluent way.
		 *
		 * @param name
		 *   the name of the field
		 * @return
		 *   builder that can be used to define the sort order
		 */
		FieldSortBuilder<Self> sort(String name);

		/**
		 * Sort on the given field and direction.
		 *
		 * @param sort
		 * @return
		 */
		Self sort(FieldSort sort);
	}

	/**
	 * Builder for {@link SearchIndexQuery.Limited} that supports using an
	 * offset and limit to paginate results.
	 */
	interface LimitableBuilder<T>
		extends BaseBuilder<LimitableBuilder<T>>, LimitableQuery.Builder<LimitableBuilder<T>>
	{
		/**
		 * Build the query.
		 *
		 * @return
		 */
		SearchIndexQuery.Limited<T> build();
	}

	/**
	 * Builder for {@link SearchIndexQuery}. Used to create the initial query
	 * that can then either be limited
	 */
	interface Builder<T>
		extends BaseBuilder<Builder<T>>, LimitableQuery.Builder<LimitableBuilder<T>>
	{
		/**
		 * Switch to returning limited results, using the default offset and
		 * limit.
		 *
		 * @return
		 */
		LimitableBuilder<T> limited();
	}
}
