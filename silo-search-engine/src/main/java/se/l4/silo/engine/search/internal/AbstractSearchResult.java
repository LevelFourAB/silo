package se.l4.silo.engine.search.internal;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.results.IterableFetchResult;
import se.l4.silo.search.Facets;
import se.l4.silo.search.PaginatedSearchResult;
import se.l4.silo.search.SearchHit;
import se.l4.silo.search.SearchResult;

public class AbstractSearchResult<T>
	extends IterableFetchResult<SearchHit<T>>
	implements SearchResult<T>
{
	private final long total;
	private final boolean estimatedTotal;
	private final Facets facets;

	public AbstractSearchResult(
		ListIterable<SearchHit<T>> iterable,
		long total,
		boolean estimatedTotal,
		Facets facets
	)
	{
		super(iterable);

		this.total = total;
		this.estimatedTotal = estimatedTotal;
		this.facets = facets;
	}

	@Override
	public Facets facets()
	{
		return facets;
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
			Facets facets
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
