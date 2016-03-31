package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.SearchFields;

public class UserQueryParserSpi
	implements QueryParser<String>
{

	@Override
	public String id()
	{
		return "user";
	}

	@Override
	public Query parse(QueryParseEncounter<String> encounter)
		throws IOException
	{
		UserQueryParser userQueryParser = new UserQueryParser(encounter.def(), false);
		for(FieldDefinition fd : encounter.def().getFields())
		{
			if(fd.getType() == SearchFields.TEXT)
			{
				userQueryParser.addField(fd.getName(), 1f);
			}
		}
		
		return userQueryParser.parse(encounter);
	}

}
