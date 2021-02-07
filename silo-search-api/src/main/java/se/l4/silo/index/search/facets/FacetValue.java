package se.l4.silo.index.search.facets;

/**
 * Value within a {@link FacetResult}.
 */
public interface FacetValue<V>
{
	/**
	 * Get the number of matches for a facet.
	 *
	 * @return
	 */
	int getCount();

	/**
	 * Get the item describing the value.
	 *
	 * @return
	 */
	V getItem();
}
