package se.l4.silo.index.search.facets;

import java.util.OptionalInt;

/**
 * Activation of faceting for a certain request.
 */
public interface FacetQuery<V>
	extends FacetRef<V>
{
	/**
	 * Get the facet being activated.
	 *
	 * @return id
	 */
	String getId();

	/**
	 * Get limit for how many facets to return.
	 *
	 * @return
	 */
	OptionalInt getLimit();
}
