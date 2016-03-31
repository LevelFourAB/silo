package se.l4.silo.engine.search;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;

/**
 * Information about a supported language.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Language
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
	
	/**
	 * Get the analyzer to use when working with text.
	 * 
	 * @return
	 */
	Analyzer getTextAnalyzer(boolean stopwords);
	
	/**
	 * Get the analyzer to use when using prefix queries. This analyzer
	 * should match the {@link #getTextAnalyzer() text analyzer}, but in most
	 * cases it will skip stop words.
	 * 
	 * @return
	 */
	Analyzer getPrefixAnalyzer();
	
	/**
	 * Get the analyzer to use for query and spell suggestions. This analyzer
	 * should be as simple as possible, as the tokens produced should be human
	 * readable.
	 * 
	 * <p>
	 * Recommended: Tokenization, lowercase, remove stopwords 
	 *  
	 * @return
	 */
	Analyzer getSuggestAnalyzer();
}
