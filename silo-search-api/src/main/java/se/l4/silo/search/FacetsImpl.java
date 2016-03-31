package se.l4.silo.search;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class FacetsImpl
	implements Facets
{
	private final ArrayListMultimap<String, FacetEntry> items;

	public FacetsImpl()
	{
		items = ArrayListMultimap.create();
	}

	@Override
	public List<FacetEntry> get(String id)
	{
		return items.get(id);
	}
	
	public void add(String facet, String label, int count, Object extra)
	{
		items.put(facet, new DefaultFacetEntry(label, count, extra));
	}
	
	public void addAll(String facet, List<FacetEntry> entries)
	{
		items.putAll(facet, entries);
	}
}
