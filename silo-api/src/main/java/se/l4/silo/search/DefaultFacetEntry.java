package se.l4.silo.search;

public class DefaultFacetEntry
	implements FacetEntry
{
	private String label;
	private int count;
	private Object data;

	public DefaultFacetEntry(String label, int count, Object data)
	{
		this.label = label;
		this.count = count;
		this.data = data;
	}
	
	@Override
	public String label()
	{
		return label;
	}
	
	@Override
	public int count()
	{
		return count;
	}
	
	@Override
	public Object data()
	{
		return data;
	}
	
	@Override
	public String toString()
	{
		return "DefaultFacetEntry{" + label + ", count=" + count + ", data=" + data + "}";
	}
}