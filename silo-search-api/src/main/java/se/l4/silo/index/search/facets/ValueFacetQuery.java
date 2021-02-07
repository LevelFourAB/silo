package se.l4.silo.index.search.facets;

import se.l4.silo.index.search.internal.ValueFacetQueryImpl;

/**
 * Query for distinct values.
 */
public interface ValueFacetQuery<V>
	extends FacetQuery<V>
{
	public static <V> Builder<V> create(String id, Class<V> valueType)
	{
		return ValueFacetQueryImpl.create(id, valueType);
	}

	interface Builder<V>
		extends FacetRef<V>
	{
		/**
		 * Limit to the top number of entries.
		 *
		 * @param topN
		 * @return
		 */
		Builder<V> withLimit(int topN);

		/**
		 * Build the query.
		 *
		 * @return
		 */
		ValueFacetQuery<V> build();
	}
}
