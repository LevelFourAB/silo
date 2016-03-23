package se.l4.silo.index;

import se.l4.silo.query.IndexQuery;
import se.l4.silo.query.QueryRunner;
import se.l4.silo.query.QueryType;

public class IndexQueryType<ResultType>
	implements QueryType<ResultType, ResultType, IndexQuery<ResultType>>
{

	@Override
	public IndexQuery<ResultType> create(QueryRunner<ResultType, ResultType> runner)
	{
		return new IndexQueryImpl<>(runner);
	}

}
