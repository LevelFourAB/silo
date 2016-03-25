package se.l4.silo.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.builder.SearchIndexBuilder;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.config.SearchIndexConfig;
import se.l4.silo.engine.internal.search.SearchIndexBuilderImpl;
import se.l4.silo.engine.internal.search.SearchIndexQueryEngine;

public class SearchIndexQueryEngineFactory
	implements QueryEngineFactory<SearchIndexBuilder<?>, SearchIndexConfig>
{
	private static final SearchIndexQueryEngineFactory INSTANCE = new SearchIndexQueryEngineFactory();
	
	@Override
	public String getId()
	{
		return "silo:search-index";
	}

	@Override
	public <T> SearchIndexBuilder<?> builder(Function<QueryEngineConfig, T> configReceiver)
	{
		return new SearchIndexBuilderImpl<>(configReceiver);
	}

	@Override
	public Class<SearchIndexConfig> getConfigClass()
	{
		return SearchIndexConfig.class;
	}

	@Override
	public QueryEngine<?> create(QueryEngineCreationEncounter<SearchIndexConfig> encounter)
	{
		Path path = encounter.getDataDirectory();
		try
		{
			return new SearchIndexQueryEngine(path, encounter.getConfig());
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to setup search index; " + e.getMessage(), e);
		}
	}

	/**
	 * Get an instance of this factory for use with builders.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> QueryEngineFactory<SearchIndexBuilder<T>, ?> type()
	{
		return (QueryEngineFactory) INSTANCE;
	}
}
