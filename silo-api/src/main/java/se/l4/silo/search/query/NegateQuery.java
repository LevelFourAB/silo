package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;
import se.l4.silo.search.QueryPart;
import se.l4.silo.search.QueryWithSubquery;

public class NegateQuery<R>
	extends AbstractQueryPart<R>
	implements QueryWithSubquery<NegateQuery<R>, R>
{
	public NegateQuery()
	{
	}
	
	@Override
	public void addQuery(QueryItem item)
	{
		receiver.addQuery(new QueryItem("negate", item));
	}
	
	@Override
	public <P extends QueryPart<R>> P query(P q)
	{
		q.parent(parent, this);
		return q;
	}
}
