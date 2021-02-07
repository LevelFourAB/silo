package se.l4.silo.index.search.facets;

/**
 * Reference to a specific facet. Can be used to define up some specific
 */
public interface FacetRef<V>
{
	/**
	 * Get the identifier of the facet. This should match the id as used in
	 * {@link FacetQuery}.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Get the value type.
	 *
	 * @return
	 */
	Class<V> getValueType();
}
