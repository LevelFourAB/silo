package se.l4.silo.engine.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineCreationEncounter;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.search.internal.SearchEngine;
import se.l4.silo.engine.search.internal.SearchIndexBuilderImpl;
import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;

public class SearchIndexQueryEngineFactory
	implements QueryEngineFactory<SearchIndexBuilder<?>, SearchIndexConfig>
{
	private final SearchEngine engine;

	public SearchIndexQueryEngineFactory(SearchEngine engine)
	{
		this.engine = engine;
	}
	
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
			return new SearchIndexQueryEngine(engine, path, encounter.getConfig());
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to setup search index; " + e.getMessage(), e);
		}
	}
}
