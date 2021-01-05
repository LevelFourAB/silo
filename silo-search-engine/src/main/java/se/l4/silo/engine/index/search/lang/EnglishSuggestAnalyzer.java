package se.l4.silo.engine.index.search.lang;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishSuggestAnalyzer
	extends Analyzer
{

	@Override
	protected TokenStreamComponents createComponents(String fieldName)
	{
		Tokenizer source = new StandardTokenizer();
		TokenStream result = new EnglishPossessiveFilter(source);
		result = new LowerCaseFilter(result);

		return new TokenStreamComponents(source, result);
	}

}
