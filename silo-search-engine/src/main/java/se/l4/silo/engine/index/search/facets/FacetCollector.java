package se.l4.silo.engine.index.search.facets;

import java.io.IOException;

/**
 * Collector of facet values.
 */
public interface FacetCollector<V>
{
	/**
	 * Collect values for the given encounter.
	 *
	 * @param encounter
	 */
	void collect(FacetCollectionEncounter<V> encounter)
		throws IOException;
}
