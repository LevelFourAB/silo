package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class SuggestQuery<R>
	extends AbstractQueryPart<R>
{
	private final String field;

	public SuggestQuery(String field)
	{
		this.field = field;
	}
	
	public R text(String text)
	{
		receiver.addQuery(new QueryItem("suggest", new SuggestQueryData(field, text)));
		return parent;
	}
}
