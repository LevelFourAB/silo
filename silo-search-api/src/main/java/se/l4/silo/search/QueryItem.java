package se.l4.silo.search;

public class QueryItem
{
	private String type;
	private Object payload;

	public QueryItem(String type, Object payload)
	{
		this.type = type;
		this.payload = payload;
	}

	public String getType()
	{
		return type;
	}

	public Object getPayload()
	{
		return payload;
	}

	@Override
	public String toString()
	{
		return "QueryItem [type=" + type + ", payload=" + payload + "]";
	}
}
