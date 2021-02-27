package se.l4.silo.engine.index.search.locales;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.icu.ICUNormalizer2FilterFactory;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;

import se.l4.silo.index.search.SearchIndexException;

/**
 * Implementation of English for Silo.
 */
public class EnglishLocaleSupport
	implements LocaleSupport
{
	public static final LocaleSupport INSTANCE = new EnglishLocaleSupport();

	private final Locale locale;

	private final Analyzer textAnalyzer;
	private final Analyzer typeAheadAnalyzer;

	public EnglishLocaleSupport()
	{
		locale = Locale.ENGLISH;

		try
		{
			textAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(ICUTokenizerFactory.class)
				.addTokenFilter(EnglishPossessiveFilterFactory.class)
				.addTokenFilter(ICUNormalizer2FilterFactory.class)
				.addTokenFilter(StopFilterFactory.class)
				.addTokenFilter(PorterStemFilterFactory.class)
				.build();

			typeAheadAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(ICUTokenizerFactory.class)
				.addTokenFilter(EnglishPossessiveFilterFactory.class)
				.addTokenFilter(ICUNormalizer2FilterFactory.class)
				.addTokenFilter(StopFilterFactory.class)
				.addTokenFilter(PorterStemFilterFactory.class)
				.addTokenFilter(NGramFilterFactory.class, new HashMap<>(Map.of(
					"minGramSize", "2",
					"maxGramSize", "5"
				)))
				.build();
		}
		catch(IOException e)
		{
			throw new SearchIndexException("Could not initialize; " + e.getMessage(), e);
		}
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

	@Override
	public Analyzer getTypeAheadAnalyzer()
	{
		return typeAheadAnalyzer;
	}
}
