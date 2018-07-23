package se.l4.silo.engine.search.internal;

import java.util.Locale;
import java.util.Map;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchEngineBuilder;
import se.l4.silo.engine.search.query.QueryParser;

/**
 * Search engine as created from {@link SearchEngineBuilder}. This contains
 * information shared between instances of {@link SearchIndexQueryEngine}
 * such as information about languages and query parsers.
 *
 * @author Andreas Holstenson
 *
 */
public class SearchEngine
{
	private final Locale defaultLanguage;
	private final Map<String, Language> languages;
	private final Map<String, QueryParser<?>> queryParsers;

	public SearchEngine(Locale defaultLanguage,
			Map<String, Language> languages,
			Map<String, QueryParser<?>> queryParsers)
	{
		this.defaultLanguage = defaultLanguage;
		this.languages = languages;
		this.queryParsers = queryParsers;
	}

	/**
	 * Get the default language for indexes under this engine.
	 *
	 * @return
	 */
	public Locale getDefaultLanguage()
	{
		return defaultLanguage;
	}

	/**
	 * Check if the given locale is supported.
	 *
	 * @param locale
	 * @return
	 */
	public boolean isSupportedLanguage(Locale locale)
	{
		return languages.containsKey(locale.toLanguageTag());
	}

	/**
	 * Get the language that matches the given locale.
	 *
	 * @param locale
	 * @return
	 */
	public Language getLanguage(Locale locale)
	{
		return languages.get(locale.toLanguageTag());
	}

	/**
	 * Fetch a query parser with the given type.
	 *
	 * @param type
	 * @return
	 */
	public QueryParser<?> queryParser(String type)
	{
		return queryParsers.get(type);
	}
}
