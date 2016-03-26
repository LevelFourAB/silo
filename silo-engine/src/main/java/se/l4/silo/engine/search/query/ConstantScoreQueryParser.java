package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.search.query.ConstantScoreData;

public class ConstantScoreQueryParser
	implements QueryParser<ConstantScoreData>
{

	@Override
	public String id()
	{
		return "constantScore";
	}

	@Override
	public Query parse(QueryParseEncounter<ConstantScoreData> encounter)
		throws IOException
	{
		ConstantScoreData data = encounter.data();
		ConstantScoreQuery q = new ConstantScoreQuery(encounter.parse(data.getSubQuery()));
		if(data.getScore() == 1)
		{
			return q;
		}
		
		return new BoostQuery(q, data.getScore());
	}

}
