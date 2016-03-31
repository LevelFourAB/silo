package se.l4.silo.engine.search.internal;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchEngineBuilder;
import se.l4.silo.engine.search.SearchIndexQueryEngineFactory;
import se.l4.silo.engine.search.lang.EnglishLanguage;
import se.l4.silo.engine.search.query.AndQueryParser;
import se.l4.silo.engine.search.query.ConstantScoreQueryParser;
import se.l4.silo.engine.search.query.NegateQueryParser;
import se.l4.silo.engine.search.query.OrQueryParser;
import se.l4.silo.engine.search.query.QueryParser;
import se.l4.silo.engine.search.query.RangeQueryParser;
import se.l4.silo.engine.search.query.StandardQueryParser;
import se.l4.silo.engine.search.query.SuggestQueryParser;
import se.l4.silo.engine.search.query.UserQueryParserSpi;

public class SearchEngineBuilderImpl
	implements SearchEngineBuilder
{
	private final Map<String, Language> langs;
	private final Map<String, QueryParser<?>> queryTypes;
	
	public SearchEngineBuilderImpl()
	{
		langs = new HashMap<>();
		addLanguage(new EnglishLanguage());
		
		queryTypes = new HashMap<>();
		addQueryParser(new AndQueryParser());
		addQueryParser(new OrQueryParser());
		addQueryParser(new ConstantScoreQueryParser());
		addQueryParser(new NegateQueryParser());
		addQueryParser(new RangeQueryParser());
		addQueryParser(new StandardQueryParser());
		addQueryParser(new SuggestQueryParser());
		addQueryParser(new UserQueryParserSpi());
	}

	@Override
	public SearchEngineBuilder addLanguage(Language language)
	{
		langs.put(language.getLocale().toLanguageTag(), language);
		return this;
	}
	
	public SearchEngineBuilder addQueryParser(QueryParser<?> parser)
	{
		queryTypes.put(parser.id(), parser);
		
		return this;
	}

	@Override
	public SearchIndexQueryEngineFactory build()
	{
		SearchEngine engine = new SearchEngine(ImmutableMap.copyOf(langs), ImmutableMap.copyOf(queryTypes));
		return new SearchIndexQueryEngineFactory(engine);
	}

}
