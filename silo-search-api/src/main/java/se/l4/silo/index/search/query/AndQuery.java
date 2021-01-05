package se.l4.silo.index.search.query;

import org.eclipse.collections.api.RichIterable;

import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.internal.AndQueryImpl;

public interface AndQuery
	extends QueryBranch
{
	RichIterable<? extends QueryClause> getItems();

	static Builder create()
	{
		return AndQueryImpl.create();
	}

	interface Builder
		extends QueryBranch.Builder<Builder>
	{
		/**
		 * Build the final query.
		 *
		 * @return
		 */
		AndQuery build();
	}
}
