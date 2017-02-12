package se.l4.silo.search.query;

public class UserQueryData
{
	private final String[] fields;
	private final float[] boosts;
	private final String query;
	
	public UserQueryData(String[] fields, float[] boosts, String query)
	{
		this.fields = fields;
		this.boosts = boosts;
		this.query = query;
	}

	public String[] getFields()
	{
		return fields;
	}
	
	public float[] getBoosts()
	{
		return boosts;
	}
	
	public String getQuery()
	{
		return query;
	}
}
