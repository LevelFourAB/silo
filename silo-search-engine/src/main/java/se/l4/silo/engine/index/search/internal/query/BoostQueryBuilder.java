package se.l4.silo.engine.index.search.internal.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.index.search.query.BoostQuery;

/**
 * Builder that handles {@link BoostQuery}.
 */
public class BoostQueryBuilder
	implements QueryBuilder<BoostQuery>
{
	@Override
	public Query parse(QueryEncounter<BoostQuery> encounter)
		throws IOException
	{
		BoostQuery instance = encounter.clause();
		return new org.apache.lucene.search.BoostQuery(
			encounter.parse(instance.getClause()),
			instance.getBoost()
		);
	}
}
