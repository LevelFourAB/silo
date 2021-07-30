package se.l4.silo.index.search.internal;

import se.l4.silo.index.search.query.UserQuery;
import se.l4.silo.index.search.query.UserQueryMatcher;
import se.l4.silo.index.search.query.UserQuery.Context;

public class UserQueryMatcherImpl
	implements UserQueryMatcher
{
	private final UserQuery.Context context;
	private final String query;

	public UserQueryMatcherImpl(UserQuery.Context context, String query)
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
}
