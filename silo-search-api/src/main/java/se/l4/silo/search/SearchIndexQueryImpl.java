package se.l4.silo.search;

import java.util.Locale;
import java.util.function.Supplier;

import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryRunner;
import se.l4.silo.search.facet.FacetQueryType;
import se.l4.silo.search.query.QueryReceiver;

public class SearchIndexQueryImpl<T>
	implements SearchIndexQuery<T>
{
	private final QueryRunner<T, SearchHit<T>> runner;
	private SearchIndexQueryRequest request;

	public SearchIndexQueryImpl(QueryRunner<T, SearchHit<T>> runner)
	{
		this.runner = runner;
		request = new SearchIndexQueryRequest();
	}

	@Override
	public SearchIndexQuery<T> fromLocale(Locale locale)
	{
		request.setLanguage(locale.toLanguageTag());
		return this;
	}

	@Override
	public SearchIndexQuery<T> waitForLatest()
	{
		request.setWaitForLatest(true);
		return this;
	}

	@Override
	public SearchIndexQuery<T> offset(long offset)
	{
		request.setOffset(offset);
		return this;
	}

	@Override
	public SearchIndexQuery<T> limit(long limit)
	{
		request.setLimit(limit);
		return this;
	}

	@Override
	public void parent(SearchIndexQuery<T> path, QueryReceiver receiver)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addQuery(QueryItem item)
	{
		request.addQueryItem(item);
	}

	@Override
	public SearchIndexQuery<T> done()
	{
		return this;
	}

	@Override
	public <P extends QueryPart<SearchIndexQuery<T>>> P query(P q)
	{
		q.parent(this, this);
		return q;
	}

	@Override
	public <C extends FacetQueryBuilder<SearchIndexQuery<T>>> C withFacet(String id, Supplier<C> facetType)
	{
		C builder = facetType.get();
		builder.setReceiver(id, c -> {
			request.addFacetItem(c);
			return this;
		});
		return builder;
	}

	@Override
	public SearchIndexQuery<T> addSort(String sort, boolean sortAscending)
	{
		request.addSortItem(sort, sortAscending, null);
		return this;
	}

	@Override
	public <C extends SortingQueryBuilder<SearchIndexQuery<T>>> C addSort(String field, Supplier<C> scoring)
	{
		C builder = scoring.get();
		builder.setReceiver((ascending, params) -> {
			request.addSortItem(field, ascending, params);
			return this;
		});
		return builder;
	}

	@Override
	public <C extends ScoringQueryBuilder<SearchIndexQuery<T>>> C setScoring(Supplier<C> scoring)
	{
		C builder = scoring.get();
		builder.setReceiver(c -> {
			request.setScoring(c);
			return this;
		});
		return builder;
	}

	@Override
	public <C extends FacetQueryBuilder<SearchIndexQuery<T>>> C withFacet(String id, FacetQueryType<SearchIndexQuery<T>, C> type)
	{
		return type.create(id, c -> {
			request.addFacetItem(c);
			return this;
		});
	}

	@Override
	public SearchResult<T> run()
	{
		QueryFetchResult<SearchHit<T>> qr = runner.fetchResults(request, (f) -> {
			float score = f.getMetadata("score", 0f);
			return new SearchHitImpl<>(f.getData(), score);
		});

		Facets facets = qr.getMetadata("facets");
		if(facets == null)
		{
			facets = new FacetsImpl();
		}

		return new SearchResultImpl<>(qr, facets);
	}
}
