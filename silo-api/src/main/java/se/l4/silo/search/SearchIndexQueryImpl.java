package se.l4.silo.search;

import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryRunner;
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
	public FacetQueryBuilder withFacet(String id)
	{
		return null;
	}
	
	@Override
	public SearchResult<T> run()
	{
		QueryFetchResult<SearchHit<T>> qr = runner.fetchResults(request, (f) -> {
			return new SearchHitImpl<>(f.getData()); 
		});
		
		return new SearchResultImpl<>(qr);
	}

}
