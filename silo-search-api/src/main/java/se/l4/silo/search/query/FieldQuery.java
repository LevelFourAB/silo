package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class FieldQuery<R>
	extends AbstractQueryPart<R>
{
	private String field;

	public FieldQuery(String field)
	{
		this.field = field;
	}
	
	public R is(Object value)
	{
		receiver.addQuery(new QueryItem("field", new FieldQueryData(field, value)));
		return parent;
	}
}
