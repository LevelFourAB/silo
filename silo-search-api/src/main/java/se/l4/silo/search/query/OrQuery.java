package se.l4.silo.search.query;

import java.util.List;

import com.google.common.collect.Lists;

import se.l4.silo.search.QueryItem;
import se.l4.silo.search.QueryPart;
import se.l4.silo.search.QueryWithSubqueries;

public class OrQuery<ReturnPath>
	extends AbstractQueryPart<ReturnPath>
	implements QueryWithSubqueries<OrQuery<ReturnPath>, ReturnPath>
{
	private final List<QueryItem> items;

	public OrQuery()
	{
		items = Lists.newArrayList();
	}

	@Override
	public void addQuery(QueryItem item)
	{
		items.add(item);
	}

	@Override
	public <P extends QueryPart<OrQuery<ReturnPath>>> P query(P q)
	{
		q.parent(this, this);
		return q;
	}

	@Override
	public ReturnPath done()
	{
		receiver.addQuery(new QueryItem("or", items));
		return parent;
	}
}
