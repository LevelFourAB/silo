package se.l4.silo.engine.index.search.lang;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishAnalyzer
	extends Analyzer
{
	private final boolean stopwords;

	public EnglishAnalyzer(boolean stopwords)
	{
		this.stopwords = stopwords;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName)
	{
		Tokenizer source = new StandardTokenizer();
		TokenStream result = new EnglishPossessiveFilter(source);
		result = new LowerCaseFilter(result);

		if(stopwords)
		{
			result = new StopFilter(result, org.apache.lucene.analysis.en.EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
		}
		result = new PorterStemFilter(result);

		return new TokenStreamComponents(source, result);
	}

}
