package se.l4.silo.search;

/**
 * Entry as created and returned by a facet when searching. Use {@link Facets}
 * to access them.
 *
 * @author Andreas Holstenson
 *
 */
public interface FacetEntry
{
	/**
	 * Get the label of the facet.
	 *
	 * @return
	 */
	String label();

	/**
	 * Get the number of search results that would match if this facet
	 * was applied.
	 *
	 * @return
	 */
	int count();

	/**
	 * Get the data of the facet.
	 *
	 * @return
	 */
	Object data();
}
