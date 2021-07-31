package se.l4.silo.engine.index.search.query;

import java.util.Optional;

import se.l4.silo.engine.index.search.internal.query.QueryBuildersImpl;
import se.l4.silo.index.search.QueryClause;

/**
 * Collection keeping track of available {@link QueryBuilder} instances.
 */
public interface QueryBuilders
{
	/**
	 * Get a query clause for the specified type of clause.
	 *
	 * @param <C>
	 * @param clause
	 * @return
	 */
	<C extends QueryClause> Optional<QueryBuilder<C>> get(Class<C> clause);

	/**
	 * Start building a {@link QueryBuilders} instance.
	 *
	 * @return
	 */
	static Builder create()
	{
		return QueryBuildersImpl.DEFAULT_BUILDER;
	}

	/**
	 * Builder for creating an instance fo {@link QueryBuilders}.
	 */
	interface Builder
	{
		/**
		 * Add a {@link QueryBuilder} automatically resolving the {@link QueryClause}
		 * it handles via generics.
		 *
		 * @param parser
		 * @return
		 */
		Builder add(QueryBuilder<? extends QueryClause> parser);

		/**
		 * Add a {@link QueryBuilder} for the given {@link QueryClause}.
		 *
		 * @param <C>
		 * @param clause
		 * @param parser
		 * @return
		 */
		<C extends QueryClause> Builder add(Class<C> clause, QueryBuilder<C> parser);

		/**
		 * Build the instance.
		 *
		 * @return
		 */
		QueryBuilders build();
	}
}
