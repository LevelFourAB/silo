package se.l4.silo.search;

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
	public SearchIndexQuery<T> waitForLatest()
	{
		request.setWaitForLatest(true);
		return this;
	}
	
	@Override
	public SearchIndexQuery<T> offset(int offset)
	{
		request.setOffset(offset);
		return this;
	}
	
	@Override
	public SearchIndexQuery<T> limit(int limit)
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
		request.addSortItem(sort, sortAscending);
		return this;
	}
	
	@Override
	public SearchIndexQuery<T> setScoring(String scoring)
	{
		request.setScoring(scoring);
		return this;
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
			return new SearchHitImpl<>(f.getData()); 
		});
		
		Facets facets = qr.getMetadata("facets");
		if(facets == null)
		{
			facets = new FacetsImpl();
		}
		
		return new SearchResultImpl<>(qr, facets);
	}
}
