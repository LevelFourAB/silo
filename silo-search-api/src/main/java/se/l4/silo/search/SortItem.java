package se.l4.silo.search;

public class SortItem
{
	private final String field;
	private final boolean ascending;

	public SortItem(String field, boolean ascending)
	{
		this.field = field;
		this.ascending = ascending;
	}
	
	public String getField()
	{
		return field;
	}
	
	public boolean isAscending()
	{
		return ascending;
	}
}