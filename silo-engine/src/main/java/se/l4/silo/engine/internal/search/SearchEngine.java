package se.l4.silo.engine.internal.search;

import java.util.Locale;
import java.util.Map;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.query.QueryParser;

public class SearchEngine
{
	private final Map<String, Language> languages;
	private final Map<String, QueryParser<?>> queryParsers;

	public SearchEngine(Map<String, Language> languages, Map<String, QueryParser<?>> queryParsers)
	{
		this.languages = languages;
		this.queryParsers = queryParsers;
	}
	
	public boolean isSupportedLanguage(Locale locale)
	{
		return languages.containsKey(locale.toLanguageTag());
	}
	
	public Language getLanguage(Locale locale)
	{
		return languages.get(locale.toLanguageTag());
	}

	public QueryParser<?> queryParser(String type)
	{
		return queryParsers.get(type);
	}
}
