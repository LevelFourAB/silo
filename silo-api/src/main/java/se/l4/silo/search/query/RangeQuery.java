package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class RangeQuery<R>
	extends AbstractQueryPart<R>
{
	private final String field;

	public RangeQuery(String field)
	{
		this.field = field;
	}
	
	public R is(long l)
	{
		return range(l, l);
	}
	
	public R range(long from, long to)
	{
		receiver.addQuery(new QueryItem("range", new RangeQueryData(field, from, to)));
		return parent;
	}
}
