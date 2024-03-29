package se.l4.silo.engine.index.search.internal.query;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.index.search.query.NegateQuery;

/**
 * Builder that handles {@link NegateQuery}.
 */
public class NegateQueryBuilder
	implements QueryBuilder<NegateQuery>
{
	@Override
	public Query parse(QueryEncounter<NegateQuery> encounter)
		throws IOException
	{
		return new BooleanQuery.Builder()
			.add(encounter.parse(encounter.clause().getClause()), BooleanClause.Occur.MUST_NOT)
			.build();
	}
}
