package se.l4.silo.index.search;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.index.search.facets.FacetRef;
import se.l4.silo.index.search.facets.FacetResult;
import se.l4.silo.results.SizeAwareResult;
import se.l4.silo.results.TotalAwareResult;

public interface SearchResult<T>
	extends SizeAwareResult<SearchHit<T>>, TotalAwareResult<SearchHit<T>>
{
	/**
	 * Get all the facets in this result.
	 *
	 * @return
	 */
	Flux<FacetResult<?>> facets();

	/**
	 * Get a single facet of this result.
	 *
	 * @param <V>
	 * @param ref
	 * @return
	 */
	<V> Mono<FacetResult<V>> facet(FacetRef<V> ref);

	/**
	 * Get a single facet of this result.
	 *
	 * @param <V>
	 * @param id
	 * @param type
	 * @return
	 */
	<V> Mono<FacetResult<V>> facet(String id, Class<V> type);

	/**
	 * Get if the {@link #getTotal()} is an estimated number.
	 *
	 * @return
	 */
	boolean isEstimatedTotal();
}
