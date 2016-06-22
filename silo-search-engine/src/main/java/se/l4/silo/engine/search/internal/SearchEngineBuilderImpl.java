package se.l4.silo.engine.search.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchEngineBuilder;
import se.l4.silo.engine.search.SearchIndex;
import se.l4.silo.engine.search.lang.EnglishLanguage;
import se.l4.silo.engine.search.query.AndQueryParser;
import se.l4.silo.engine.search.query.ConstantScoreQueryParser;
import se.l4.silo.engine.search.query.FieldQueryParser;
import se.l4.silo.engine.search.query.NegateQueryParser;
import se.l4.silo.engine.search.query.OrQueryParser;
import se.l4.silo.engine.search.query.QueryParser;
import se.l4.silo.engine.search.query.RangeQueryParser;
import se.l4.silo.engine.search.query.StandardQueryParser;
import se.l4.silo.engine.search.query.SuggestQueryParser;
import se.l4.silo.engine.search.query.UserQueryParserSpi;

/**
 * Implementation of {@link SearchEngineBuilder}.
 * 
 * @author Andreas Holstenson
 *
 */
public class SearchEngineBuilderImpl
	implements SearchEngineBuilder
{
	private Locale defaultLanguage;
	
	private final Map<String, Language> langs;
	private final Map<String, QueryParser<?>> queryTypes;
	
	public SearchEngineBuilderImpl()
	{
		defaultLanguage = Locale.ENGLISH;
		
		langs = new HashMap<>();
		addLanguage(new EnglishLanguage());
		
		queryTypes = new HashMap<>();
		addQueryParser(new AndQueryParser());
		addQueryParser(new OrQueryParser());
		addQueryParser(new FieldQueryParser());
		addQueryParser(new ConstantScoreQueryParser());
		addQueryParser(new NegateQueryParser());
		addQueryParser(new RangeQueryParser());
		addQueryParser(new StandardQueryParser());
		addQueryParser(new SuggestQueryParser());
		addQueryParser(new UserQueryParserSpi());
	}
	
	@Override
	public SearchEngineBuilder setDefaultLanguage(Locale locale)
	{
		Objects.requireNonNull(locale, "locale must be specified");
		this.defaultLanguage = locale;
		return this;
	}

	@Override
	public SearchEngineBuilder addLanguage(Language language)
	{
		langs.put(language.getLocale().toLanguageTag(), language);
		return this;
	}
	
	@Override
	public SearchEngineBuilder addQueryParser(QueryParser<?> parser)
	{
		queryTypes.put(parser.id(), parser);
		
		return this;
	}

	@Override
	public QueryEngineFactory<?, ?> build()
	{
		SearchEngine engine = new SearchEngine(defaultLanguage,
			ImmutableMap.copyOf(langs),
			ImmutableMap.copyOf(queryTypes)
		);
		return new SearchIndex(engine);
	}

}
