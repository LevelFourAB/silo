package se.l4.silo.index.search.query;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;

import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.internal.UserQueryImpl;

public interface UserQuery
	extends QueryClause
{
	enum Context
	{
		STANDARD,

		TYPE_AHEAD
	}

	/**
	 * Get the fields to search in.
	 *
	 * @return
	 */
	ImmutableList<String> getFields();

	/**
	 * Get boosts for fields given by {@link #getFields()}.
	 *
	 * @return
	 */
	ImmutableFloatList getBoosts();

	/**
	 * Get the context of this query.
	 *
	 * @return
	 */
	Context getContext();

	/**
	 * Get the query to match against.
	 *
	 * @return
	 */
	String getQuery();

	static Builder create()
	{
		return UserQueryImpl.create();
	}

	static Matcher matcher(String query)
	{
		return matcher(query, Context.STANDARD);
	}

	static Matcher matcher(String query, Context context)
	{
		return UserQueryImpl.matcher(query, context);
	}

	/**
	 * Matcher that can be used to easily perform a {@link UserQuery} on a
	 * single field.
	 */
	interface Matcher
		extends se.l4.silo.index.Matcher
	{
		/**
		 * Get the context of this query.
		 *
		 * @return
		 */
		Context getContext();

		/**
		 * The query submitted.
		 *
		 * @return
		 */
		String getQuery();
	}

	public interface Builder
	{
		/**
		 * Add a field to query.
		 *
		 * @param field
		 * @return
		 */
		Builder addField(String field);

		/**
		 * Add a field to query.
		 *
		 * @param field
		 * @param boost
		 * @return
		 */
		Builder addField(String field, float boost);

		/**
		 * Set the query.
		 *
		 * @param query
		 * @return
		 */
		Builder withQuery(String query);

		/**
		 * Set the context of the query.
		 *
		 */
		Builder withContext(Context context);

		UserQuery build();
	}
}
