package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.search.QueryClause;
import se.l4.silo.search.query.AndQuery;

public class AndQueryBuilder
	implements QueryBuilder<AndQuery>
{
	@Override
	public Query parse(QueryEncounter<AndQuery> encounter)
		throws IOException
	{
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for(QueryClause clause : encounter.clause().getItems())
		{
			builder.add(encounter.parse(clause), BooleanClause.Occur.MUST);
		}
		return builder.build();
	}
}
