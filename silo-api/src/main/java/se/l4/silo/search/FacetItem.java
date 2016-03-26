package se.l4.silo.search;

import java.util.Map;

public class FacetItem
{
	private final String id;
	private final Map<String, String> parameters;
	
	public FacetItem(String id, Map<String, String> parameters)
	{
		this.id = id;
		this.parameters = parameters;
	}
	
	public String getId()
	{
		return id;
	}
	
	public Map<String, String> getParameters()
	{
		return parameters;
	}
}