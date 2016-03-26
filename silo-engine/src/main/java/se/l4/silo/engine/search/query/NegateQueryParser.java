package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.search.QueryItem;

public class NegateQueryParser
	implements QueryParser<QueryItem>
{

	@Override
	public String id()
	{
		return "negate";
	}

	@Override
	public Query parse(QueryParseEncounter<QueryItem> encounter)
		throws IOException
	{
		return new BooleanQuery.Builder()
			.add(encounter.parse(encounter.data()), BooleanClause.Occur.MUST_NOT)
			.build();
	}

}
