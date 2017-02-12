package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class UserQuery<R>
	extends AbstractQueryPart<R>
{
	private final String[] fields;
	private float[] boosts;

	public UserQuery(String... fields)
	{
		this.fields = fields;
		this.boosts = new float[fields.length];
		for(int i=0, n=boosts.length; i<n; i++)
		{
			boosts[i] = 1;
		}
	}
	
	public R text(String query)
	{
		receiver.addQuery(new QueryItem("user", new UserQueryData(fields, boosts, query)));
		return parent;
	}
}
