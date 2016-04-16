package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class UserQuery<R>
	extends AbstractQueryPart<R>
{
	private final String[] fields;

	public UserQuery(String... fields)
	{
		this.fields = fields;
	}
	
	public R text(String query)
	{
		receiver.addQuery(new QueryItem("user", new UserQueryData(fields, query)));
		return parent;
	}
}
