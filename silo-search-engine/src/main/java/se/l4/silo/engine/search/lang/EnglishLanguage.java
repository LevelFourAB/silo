package se.l4.silo.engine.search.lang;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;

import se.l4.silo.engine.search.LocaleSupport;

/**
 * Implementation of English for Silo.
 */
public class EnglishLanguage
	implements LocaleSupport
{
	public static final LocaleSupport INSTANCE = new EnglishLanguage();

	private final Locale locale;
	private final Analyzer textAnalyzer;
	private final Analyzer prefixAnalyzer;
	private final Analyzer suggestAnalyzer;

	public EnglishLanguage()
	{
		locale = Locale.ENGLISH;
		textAnalyzer = new EnglishAnalyzer(true);
		prefixAnalyzer = new EnglishAnalyzer(false);
		suggestAnalyzer = new EnglishSuggestAnalyzer();
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
	public Analyzer getTextAnalyzer(boolean stopwords)
	{
		return stopwords ? textAnalyzer : prefixAnalyzer;
	}

	@Override
	public Analyzer getPrefixAnalyzer()
	{
		return prefixAnalyzer;
	}

	@Override
	public Analyzer getSuggestAnalyzer()
	{
		return suggestAnalyzer;
	}

}
