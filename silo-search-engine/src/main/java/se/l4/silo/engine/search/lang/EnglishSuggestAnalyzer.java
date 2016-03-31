package se.l4.silo.engine.search.lang;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishSuggestAnalyzer
	extends Analyzer
{
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName)
	{
		Tokenizer source = new StandardTokenizer();
		TokenStream result = new StandardFilter(source);
		result = new EnglishPossessiveFilter(result);
		result = new LowerCaseFilter(result);
		
		return new TokenStreamComponents(source, result);
	}

}
