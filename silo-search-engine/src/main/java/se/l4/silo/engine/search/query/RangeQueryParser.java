package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.search.query.RangeQueryData;

public class RangeQueryParser
	implements QueryParser<RangeQueryData>
{

	@Override
	public String id()
	{
		return "range";
	}

	@Override
	public Query parse(QueryParseEncounter<RangeQueryData> encounter)
		throws IOException
	{
		RangeQueryData data = encounter.data();
		FieldDefinition fdef = encounter.def().getField(data.getField());
		String name = fdef.name(data.getField(), encounter.currentLanguage());
		switch(fdef.getType().getSortType())
		{
			case LONG:
				return NumericRangeQuery.newLongRange(name, data.getFrom(), data.getTo(), true, true);
			case INT:
				return NumericRangeQuery.newIntRange(name, (int) data.getFrom(), (int) data.getTo(), true, true);
			default:
				throw new IOException("Unsupported number type: " + fdef.getType().getSortType());
		}
	}

}
