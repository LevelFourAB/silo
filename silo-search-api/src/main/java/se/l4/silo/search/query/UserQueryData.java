package se.l4.silo.search.query;

public class UserQueryData
{
	private final String[] fields;
	private final String query;
	
	public UserQueryData(String[] fields, String query)
	{
		this.fields = fields;
		this.query = query;
	}

	public String[] getFields()
	{
		return fields;
	}
	
	public String getQuery()
	{
		return query;
	}
}
