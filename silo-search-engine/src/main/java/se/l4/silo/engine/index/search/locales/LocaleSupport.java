package se.l4.silo.engine.index.search.locales;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;

/**
 * Information about a supported language.
 */
public interface LocaleSupport
{
	/**
	 * Get the locale of this language.
	 *
	 * @return
	 */
	Locale getLocale();

	/**
	 * Get the analyzer to use when working with text, including removal of
	 * stop words.
	 *
	 * @return
	 */
	Analyzer getTextAnalyzer();
}
