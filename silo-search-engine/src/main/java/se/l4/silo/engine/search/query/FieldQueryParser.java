package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.search.query.FieldQueryData;

public class FieldQueryParser
	implements QueryParser<FieldQueryData>
{

	@Override
	public String id()
	{
		return "field";
	}

	@Override
	public Query parse(QueryParseEncounter<FieldQueryData> encounter)
		throws IOException
	{
		FieldQueryData data = encounter.data();
		FieldDefinition fdef = encounter.def().getField(data.getField());
		if(data.getValue() == null)
		{
			String name = fdef.nullName(data.getField());
			return new TermQuery(new Term(name, new BytesRef(BytesRef.EMPTY_BYTES)));
		}
		else
		{
			String name = fdef.name(data.getField(), encounter.currentLanguage());
			return fdef.getType().createEqualsQuery(name, data.getValue());
		}
	}

}
