package se.l4.silo.engine.index.search.types;

import java.util.function.Function;

import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.facets.ValueFacetDef;
import se.l4.silo.engine.index.search.internal.MappedSearchFieldType;

/**
 * Interface used to mark fields that can also be faceted via value. Used
 * together with {@link ValueFacetDef}.
 */
public interface FacetableSearchFieldType<V>
	extends SearchFieldType<V>
{
	/**
	 * Create a collector for facet values.
	 *
	 * @param fieldName
	 *   the field where DocValues is stored
	 * @param encounter
	 *   the encounter to use
	 * @return
	 */
	FacetCollector<V> createFacetCollector(
		SearchFieldDefinition<?> field
	);

	@Override
	default <NV> FacetableSearchFieldType<NV> map(
		Function<V, NV> toN,
		Function<NV, V> fromN
	)
	{
		return new MappedSearchFieldType.Facetable<>(this, toN, fromN);
	}
}
