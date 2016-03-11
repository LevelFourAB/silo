package se.l4.silo.index;

import se.l4.silo.IndexQuery;
import se.l4.silo.QueryRunner;
import se.l4.silo.QueryType;

public class IndexQueryType<ResultType>
	implements QueryType<ResultType, ResultType, IndexQuery<ResultType>>
{

	@Override
	public IndexQuery<ResultType> create(QueryRunner<ResultType, ResultType> runner)
	{
		return new IndexQueryImpl<>(runner);
	}

}
