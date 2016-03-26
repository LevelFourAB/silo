package se.l4.silo.search.query;

import se.l4.silo.search.QueryPart;

public abstract class AbstractQueryPart<R>
	implements QueryPart<R>
{
	protected R parent;
	protected QueryReceiver receiver;

	@Override
	public void parent(R path, QueryReceiver receiver)
	{
		this.parent = path;
		this.receiver = receiver;
	}
}
