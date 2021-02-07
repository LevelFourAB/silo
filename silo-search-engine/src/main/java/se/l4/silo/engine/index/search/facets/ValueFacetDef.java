package se.l4.silo.engine.index.search.facets;

import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.internal.facets.ValueFacetDefImpl;
import se.l4.silo.engine.index.search.types.FacetableSearchFieldType;
import se.l4.silo.index.search.facets.ValueFacetQuery;

/**
 * Facet representing distinct values. This type of facet is useful for
 * anything that has distinct values, such as product categories, user ids,
 * tags, states etc.
 */
public interface ValueFacetDef<T, V>
	extends FacetDef<T, V, ValueFacetQuery<V>>
{
	/**
	 * Start building a new category facet.
	 *
	 * @param <T>
	 * @param type
	 * @param name
	 * @return
	 */
	public static <T> Builder<T, ?> create(Class<T> type, String name)
	{
		return ValueFacetDefImpl.create(type, name);
	}

	/**
	 * Builder for instances of {@link ValueFacetDef}.
	 */
	interface Builder<T, V>
	{
		/**
		 * Set the field to use. The type of the field should be something
		 * that is {@link FacetableSearchFieldType}.
		 *
		 * @param <NV>
		 * @param field
		 * @return
		 */
		<NV> Builder<T, NV> withField(SearchFieldDefinition<T> field);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		ValueFacetDef<T, V> build();
	}
}
