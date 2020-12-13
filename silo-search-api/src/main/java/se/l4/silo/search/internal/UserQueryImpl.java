package se.l4.silo.search.internal;

import java.util.Objects;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.impl.factory.primitive.FloatLists;

import se.l4.silo.search.query.UserQuery;

public class UserQueryImpl
	implements UserQuery
{
	private final Context context;
	private final ImmutableList<String> fields;
	private final ImmutableFloatList boosts;
	private final String query;

	private UserQueryImpl(
		Context context,
		ImmutableList<String> fields,
		ImmutableFloatList boosts,
		String query
	)
	{
		this.context = context;
		this.fields = fields;
		this.boosts = boosts;
		this.query = query;
	}

	@Override
	public ImmutableList<String> getFields()
	{
		return fields;
	}

	@Override
	public ImmutableFloatList getBoosts()
	{
		return boosts;
	}

	@Override
	public Context getContext()
	{
		return context;
	}

	@Override
	public String getQuery()
	{
		return query;
	}

	public static Builder create()
	{
		return new BuilderImpl(
			Context.STANDARD,
			Lists.immutable.empty(),
			FloatLists.immutable.empty(),
			null
		);
	}

	public static Matcher matcher(String query, Context context)
	{
		Objects.requireNonNull(context, "context must be specified");
		Objects.requireNonNull(query, "query must be specified");

		return new MatcherImpl(context, query);
	}

	private static class MatcherImpl
		implements Matcher
	{
		private final Context context;
		private final String query;

		public MatcherImpl(
			Context context,
			String query
		)
		{
			this.context = context;
			this.query = query;
		}

		@Override
		public Context getContext()
		{
			return context;
		}

		@Override
		public String getQuery()
		{
			return query;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(context, query);
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj) return true;
			if(obj == null) return false;
			if(getClass() != obj.getClass()) return false;
			MatcherImpl other = (MatcherImpl) obj;
			return context == other.context
				&& Objects.equals(query, other.query);
		}

		@Override
		public String toString()
		{
			return "UserQuery.Matcher{context=" + context + ", query=" + query + "}";
		}
	}

	private static class BuilderImpl
		implements Builder
	{
		private final Context context;
		private final ImmutableList<String> fields;
		private final ImmutableFloatList boosts;
		private final String query;

		public BuilderImpl(
			Context context,
			ImmutableList<String> fields,
			ImmutableFloatList boosts,
			String query
		)
		{
			this.context = context;
			this.fields = fields;
			this.boosts = boosts;
			this.query = query;
		}

		public Builder addField(String field)
		{
			return addField(field, 1f);
		}

		public Builder addField(String field, float boost)
		{
			Objects.requireNonNull(field);

			return new BuilderImpl(
				context,
				fields.newWith(field),
				boosts.newWith(boost),
				query
			);
		}

		public Builder withQuery(String query)
		{
			return new BuilderImpl(
				context,
				fields,
				boosts,
				query
			);
		}

		@Override
		public Builder withContext(Context context)
		{
			return new BuilderImpl(
				context,
				fields,
				boosts,
				query
			);
		}

		public UserQuery build()
		{
			Objects.requireNonNull(query, "query must be specified");
			return new UserQueryImpl(context, fields, boosts, query);
		}
	}
}
