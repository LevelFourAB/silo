package se.l4.silo.search;

public class FacetItem
{
	private final String id;
	private final Object payload;
	
	public FacetItem(String id, Object payload)
	{
		this.id = id;
		this.payload = payload;
	}
	
	public String getId()
	{
		return id;
	}

	public Object getPayload()
	{
		return payload;
	}
}