package se.l4.silo.engine.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineCreationEncounter;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.search.internal.SearchEngine;
import se.l4.silo.engine.search.internal.SearchEngineBuilderImpl;
import se.l4.silo.engine.search.internal.SearchIndexBuilderImpl;
import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;

/**
 * {@link QueryEngine} implementation for search indexes.
 *
 * @author Andreas Holstenson
 *
 */
public class SearchIndex
	implements QueryEngineFactory<SearchIndexBuilder<?>, SearchIndexConfig>
{
	private final SearchEngine engine;

	public SearchIndex(SearchEngine engine)
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
		Path path = encounter.resolveDataFile(encounter.getUniqueName());
		try
		{
			Files.createDirectories(path);
			return new SearchIndexQueryEngine(engine, encounter.getExecutor(), encounter.getUniqueName(), path, encounter.getConfig());
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to setup search index; " + e.getMessage(), e);
		}
	}

	/**
	 * Start building a new factory for {@link QueryEngine query engines} that
	 * perform searches. The result can be used with
	 * {@link SiloBuilder#addQueryEngine(QueryEngineFactory)}.
	 *
	 * <p>
	 * Example usage:
	 * <pre>
	 * LocalSilo.builder()
	 * 	.addQueryEngine(SearchIndex.builder()
	 * 		.setDefaultLocale("en")
	 * 		.build()
	 * 	)
	 *  ...
	 * </pre>
	 *
	 * @return
	 */
	public static SearchEngineBuilder builder()
	{
		return new SearchEngineBuilderImpl();
	}


	/**
	 * Create a new {@link SearchIndexBuilder}. This method is used by
	 * entities when constructing indexes.
	 *
	 * @param f
	 * @return
	 */
	public static <Parent> SearchIndexBuilder<Parent> queryEngine(Function<QueryEngineConfig, Parent> configReceiver)
	{
		return new SearchIndexBuilderImpl<>(configReceiver);
	}
}
