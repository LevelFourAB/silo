package se.l4.silo.index.search.query;

import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.internal.UserQueryMatcherImpl;

/**
 * Matcher for matching a full text field against a user query.
 */
public interface UserQueryMatcher
	extends Matcher<String>
{
	/**
	 * Get the context of the query.
	 */
	UserQuery.Context getContext();

	/**
	 * Get the query.
	 *
	 * @return
	 */
	String getQuery();

	/**
	 * Create a matcher for a standard query.
	 *
	 * @param q
	 * @return
	 */
	public static UserQueryMatcher standard(String q)
	{
		return new UserQueryMatcherImpl(UserQuery.Context.STANDARD, q);
	}

	/**
	 * Create a matcher for a type-ahead query.
	 *
	 * @param q
	 * @return
	 */
	public static UserQueryMatcher typeAhead(String q)
	{
		return new UserQueryMatcherImpl(UserQuery.Context.TYPE_AHEAD, q);
	}
}
