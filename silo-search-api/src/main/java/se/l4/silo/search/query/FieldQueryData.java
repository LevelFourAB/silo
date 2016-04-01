package se.l4.silo.search.query;

public class FieldQueryData
{
	private final String field;
	private final Object value;
	
	public FieldQueryData(String field, Object value)
	{
		this.field = field;
		this.value = value;
	}
	
	public String getField()
	{
		return field;
	}

	public Object getValue()
	{
		return value;
	}
}
