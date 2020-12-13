package se.l4.silo.search.internal;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import se.l4.silo.search.QueryClause;
import se.l4.silo.search.query.OrQuery;

public class OrQueryImpl
	implements OrQuery
{
	private final ImmutableList<? extends QueryClause> items;

	public OrQueryImpl(
		Iterable<? extends QueryClause> items
	)
	{
		this.items = Lists.immutable.ofAll(items);
	}

	@Override
	public ImmutableList<? extends QueryClause> getItems()
	{
		return items;
	}

	public static Builder create()
	{
		return new BuilderImpl(Lists.immutable.empty());
	}

	private static class BuilderImpl
		implements Builder
	{
		private final ImmutableList<QueryClause> clauses;

		public BuilderImpl(
			ImmutableList<QueryClause> clauses
		)
		{
			this.clauses = clauses;
		}

		@Override
		public Builder add(Iterable<? extends QueryClause> clauses)
		{
			return new BuilderImpl(
				this.clauses.newWithAll(clauses)
			);
		}

		@Override
		public OrQuery build()
		{
			return new OrQueryImpl(clauses);
		}
	}
}
