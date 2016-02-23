package se.l4.silo.index;

import se.l4.silo.IndexQuery;
import se.l4.silo.QueryRunner;
import se.l4.silo.QueryType;

public class IndexQueryType<ResultType>
	implements QueryType<ResultType, IndexQuery<ResultType>>
{

	@Override
	public IndexQuery<ResultType> create(Class<ResultType> type, QueryRunner<ResultType> runner)
	{
		return new IndexQueryImpl<>(runner);
	}

}
