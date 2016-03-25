package se.l4.silo.search;

import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryRunner;

public class SearchIndexQueryImpl<T>
	implements SearchIndexQuery<T>
{
	private final QueryRunner<T, SearchHit<T>> runner;

	public SearchIndexQueryImpl(QueryRunner<T, SearchHit<T>> runner)
	{
		this.runner = runner;
	}

	@Override
	public SearchResult<T> run()
	{
		QueryFetchResult<SearchHit<T>> qr = runner.fetchResults(new SearchIndexQueryRequest(), (f) -> {
			return new SearchHitImpl<>(f.getData()); 
		});
		
		return new SearchResultImpl<>(qr);
	}

}
