package se.l4.silo.search;

import se.l4.silo.query.QueryRunner;
import se.l4.silo.query.QueryType;

public class SearchIndexQueryType<ResultType>
	implements QueryType<ResultType, SearchHit<ResultType>, SearchIndexQuery<ResultType>>
{
	@Override
	public SearchIndexQuery<ResultType> create(QueryRunner<ResultType, SearchHit<ResultType>> runner)
	{
		return new SearchIndexQueryImpl<>(runner);
	}
}
