package se.l4.silo.engine.index.search.locales;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;

/**
 * Implementation of English for Silo.
 */
public class EnglishLocaleSupport
	implements LocaleSupport
{
	public static final LocaleSupport INSTANCE = new EnglishLocaleSupport();

	private final Locale locale;
	private final Analyzer textAnalyzer;

	public EnglishLocaleSupport()
	{
		locale = Locale.ENGLISH;
		textAnalyzer = new DefaultAnalyzer();
	}

	@Override
	public Locale getLocale()
	{
		return locale;
	}

	@Override
	public Analyzer getTextAnalyzer()
	{
		return textAnalyzer;
	}

	private static class DefaultAnalyzer
		extends Analyzer
	{
		@Override
		protected TokenStreamComponents createComponents(String fieldName)
		{
			Tokenizer source = new ICUTokenizer();

			TokenStream stream;
			stream = new EnglishPossessiveFilter(source);
			stream = new ICUNormalizer2Filter(stream);
			stream = new StopFilter(stream, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
			stream = new PorterStemFilter(stream);

			return new TokenStreamComponents(source, stream);
		}

		@Override
		protected TokenStream normalize(String fieldName, TokenStream in)
		{
			return new ICUNormalizer2Filter(in);
		}
	}
}
