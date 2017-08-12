package se.l4.silo.search.query;

import se.l4.silo.search.QueryCriteriaBuilder;
import se.l4.silo.search.QueryItem;
import se.l4.silo.search.QueryPart;

public class NegateQuery<R>
	extends AbstractQueryPart<R>
	implements QueryCriteriaBuilder<R>
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
