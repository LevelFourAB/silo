package se.l4.silo.engine.search;

import se.l4.silo.engine.search.facets.Facet;

/**
 * Definition of a {@link Facet}.
 *
 * @author Andreas Holstenson
 *
 */
public interface FacetDefinition
{
	/**
	 * Get the id of this facet.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Get the instance to use for querying.
	 *
	 * @return
	 */
	Facet<?> getInstance();
}
