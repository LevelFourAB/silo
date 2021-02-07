package se.l4.silo.engine.index.search.types;

import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.facets.ValueFacetDef;

/**
 * Interface used to mark fields that can also be faceted via value. Used
 * together with {@link ValueFacetDef}.
 */
public interface FacetableSearchFieldType<V>
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
}
