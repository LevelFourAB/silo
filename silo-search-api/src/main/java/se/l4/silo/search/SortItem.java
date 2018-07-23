package se.l4.silo.search;

public class SortItem
{
	private final String field;
	private final boolean ascending;
	private final Object params;

	public SortItem(String field, boolean ascending, Object params)
	{
		this.field = field;
		this.ascending = ascending;
		this.params = params;
	}

	public String getField()
	{
		return field;
	}

	public boolean isAscending()
	{
		return ascending;
	}

	public Object getParams()
	{
		return params;
	}
}
