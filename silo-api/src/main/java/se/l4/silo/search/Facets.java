package se.l4.silo.search;

import java.util.List;

public interface Facets
{
	List<FacetEntry> get(String id);
}
