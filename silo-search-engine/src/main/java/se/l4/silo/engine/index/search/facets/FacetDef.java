package se.l4.silo.engine.index.search.facets;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.index.search.facets.FacetQuery;

/**
 * Definition of a facet.
 */
public interface FacetDef<T, V, Q extends FacetQuery<V>>
{
	/**
	 * Get the identifier of the facet.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Get the fields that this facet requires.
	 *
	 * @return
	 */
	ListIterable<? extends SearchFieldDefinition<T>> getFields();

	/**
	 * Create a collector for the given query.
	 *
	 * @param query
	 *   query instance
	 * @return
	 *   collector for the query
	 */
	FacetCollector<V> createCollector(Q query);
}
