package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.FieldDefinition;

public class StandardQueryParser
	implements QueryParser<String>
{
	@Override
	public String id()
	{
		return "standard";
	}

	@Override
	public Query parse(QueryParseEncounter<String> encounter)
		throws IOException
	{
		try
		{
			return new org.apache.lucene.queryparser.classic.QueryParser(null, new KeywordAnalyzer())
			{
				@Override
				protected Query newFieldQuery(org.apache.lucene.analysis.Analyzer analyzer, String field, String queryText, boolean quoted)
					throws ParseException
				{
					FieldDefinition fdef = encounter.def().getField(field);
					if(fdef != null)
					{
						field = fdef.name(field, encounter.currentLanguage());
						analyzer = fdef.getType().getAnalyzer(encounter.currentLanguage());
					}

					return super.newFieldQuery(analyzer, field, queryText, quoted);
				}
			}
			.parse(encounter.data());
		}
		catch(ParseException e)
		{
			throw new IOException(e.getMessage(), e);
		}
	}

}
