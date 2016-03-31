package se.l4.silo.engine.search.query;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.search.QueryItem;

public class AndQueryParser
	implements QueryParser<List<QueryItem>>
{

	@Override
	public String id()
	{
		return "and";
	}

	@Override
	public Query parse(QueryParseEncounter<List<QueryItem>> encounter)
		throws IOException
	{
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for(QueryItem item : encounter.data())
		{
			builder.add(encounter.parse(item), BooleanClause.Occur.MUST);
		}
		return builder.build();
	}

}
