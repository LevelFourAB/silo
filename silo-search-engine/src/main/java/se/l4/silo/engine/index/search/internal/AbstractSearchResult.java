package se.l4.silo.engine.index.search.internal;

import org.eclipse.collections.api.list.ListIterable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.index.search.PaginatedSearchResult;
import se.l4.silo.index.search.SearchHit;
import se.l4.silo.index.search.SearchResult;
import se.l4.silo.index.search.facets.FacetRef;
import se.l4.silo.index.search.facets.FacetResult;
import se.l4.silo.results.IterableFetchResult;

public class AbstractSearchResult<T>
	extends IterableFetchResult<SearchHit<T>>
	implements SearchResult<T>
{
	private final long total;
	private final boolean estimatedTotal;
	private final ListIterable<FacetResult<?>> facets;

	public AbstractSearchResult(
		ListIterable<SearchHit<T>> iterable,
		long total,
		boolean estimatedTotal,
		ListIterable<FacetResult<?>> facets
	)
	{
		super(iterable);

		this.total = total;
		this.estimatedTotal = estimatedTotal;
		this.facets = facets;
	}

	@Override
	public Flux<FacetResult<?>> facets()
	{
		return Flux.fromIterable(facets);
	}

	@Override
	public <V> Mono<FacetResult<V>> facet(FacetRef<V> ref)
	{
		return facet(ref.getId(), ref.getValueType());
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <V> Mono<FacetResult<V>> facet(String id, Class<V> type)
	{
		return (Mono) facets()
			.filter(f -> f.getId().equals(id))
			.singleOrEmpty();
	}

	@Override
	public long getTotal()
	{
		return total;
	}

	@Override
	public boolean isEstimatedTotal()
	{
		return estimatedTotal;
	}

	public static class LimitedImpl<T>
		extends AbstractSearchResult<T>
		implements PaginatedSearchResult<T>
	{
		private final long offset;
		private final long limit;

		public LimitedImpl(
			ListIterable<SearchHit<T>> iterable,
			long total,
			boolean estimatedTotal,
			long offset,
			long limit,
			ListIterable<FacetResult<?>> facets
		)
		{
			super(
				iterable,
				total,
				estimatedTotal,
				facets
			);

			this.offset = offset;
			this.limit = limit;
		}

		@Override
		public long getOffset()
		{
			return offset;
		}

		@Override
		public long getLimit()
		{
			return limit;
		}
	}
}
