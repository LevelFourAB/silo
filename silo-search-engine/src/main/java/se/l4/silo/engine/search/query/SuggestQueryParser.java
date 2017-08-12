package se.l4.silo.engine.search.query;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.search.query.SuggestQueryData;

public class SuggestQueryParser
	implements QueryParser<SuggestQueryData>
{

	@Override
	public String id()
	{
		return "suggest";
	}

	@Override
	public Query parse(QueryParseEncounter<SuggestQueryData> encounter)
		throws IOException
	{
		String field = encounter.data().getField();
		String value = encounter.data().getText();
		FieldDefinition fdef = encounter.def().getField(field);
		String name = fdef.name(field, encounter.currentLanguage());

		Analyzer analyzer = fdef.getType().getAnalyzer(encounter.currentLanguage());

		BooleanQuery.Builder q = new BooleanQuery.Builder();
	    try(TokenStream ts = analyzer.tokenStream("", new StringReader(value)))
	    {
			ts.reset();
			CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
			OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);

			String lastToken = null;
			int maxEndOffset = -1;
			while(ts.incrementToken())
			{
				if(lastToken != null)
				{
					q.add(new TermQuery(new Term(name, lastToken)), BooleanClause.Occur.MUST);
				}

				lastToken = termAtt.toString();

				if(lastToken != null)
				{
					maxEndOffset = Math.max(maxEndOffset, offsetAtt.endOffset());
				}
			}
			ts.end();

			if(lastToken != null)
			{
				Query lastQuery;
				if(maxEndOffset == offsetAtt.endOffset())
				{
					lastQuery = new PrefixQuery(new Term(name, lastToken));
				}
				else
				{
					lastQuery = new TermQuery(new Term(name, lastToken));
				}

				if(lastQuery != null)
				{
					q.add(lastQuery, BooleanClause.Occur.MUST);
				}
			}
	    }

	    return q.build();
	}

}
