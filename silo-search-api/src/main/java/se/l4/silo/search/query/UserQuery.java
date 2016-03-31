package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class UserQuery<R>
	extends AbstractQueryPart<R>
{
	public R text(String query)
	{
		receiver.addQuery(new QueryItem("user", query));
		return parent;
	}
}
