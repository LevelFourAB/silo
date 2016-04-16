package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.search.query.UserQueryData;

public class UserQueryParserSpi
	implements QueryParser<UserQueryData>
{

	@Override
	public String id()
	{
		return "user";
	}

	@Override
	public Query parse(QueryParseEncounter<UserQueryData> encounter)
		throws IOException
	{
		UserQueryParser userQueryParser = new UserQueryParser(encounter.def(), false);
		return userQueryParser.parse(encounter);
	}

}
