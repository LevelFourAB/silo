package se.l4.silo.index.search.facets;

import reactor.core.publisher.Flux;

/**
 * Values returned for a specific facet.
 */
public interface FacetResult<V>
	extends Iterable<FacetValue<V>>
{
	/**
	 * Get the identifier of the facet.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Get the values as a flux.
	 *
	 * @return
	 */
	Flux<FacetValue<V>> asFlux();
}
