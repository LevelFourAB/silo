package se.l4.silo.engine.index.search.query;

import java.util.Optional;

import se.l4.silo.engine.index.search.internal.QueryBuildersImpl;
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

	static Builder create()
	{
		return QueryBuildersImpl.DEFAULT_BUILDER;
	}

	interface Builder
	{
		Builder add(QueryBuilder<? extends QueryClause> parser);

		<C extends QueryClause> Builder add(Class<C> clause, QueryBuilder<C> parser);

		QueryBuilders build();
	}
}
