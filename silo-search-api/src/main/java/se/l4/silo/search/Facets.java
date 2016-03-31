package se.l4.silo.search;

import java.util.List;

/**
 * Information about facets for a {@link SearchResult}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Facets
{
	/**
	 * Get all {@link FacetEntry entries} for a given facet.
	 *  
	 * @param id
	 * @return
	 */
	List<FacetEntry> get(String id);
}
