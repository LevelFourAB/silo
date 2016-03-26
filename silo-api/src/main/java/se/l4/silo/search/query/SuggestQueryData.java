package se.l4.silo.search.query;

public class SuggestQueryData
{
	private final String field;
	private final String text;
	
	public SuggestQueryData(String field, String text)
	{
		this.field = field;
		this.text = text;
	}
	
	public String getField()
	{
		return field;
	}
	
	public String getText()
	{
		return text;
	}
}
