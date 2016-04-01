package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.IndexBuilder;
import se.l4.silo.engine.config.IndexConfig;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.internal.index.IndexQueryBuilderImpl;
import se.l4.silo.engine.internal.index.IndexQueryEngine;

/**
 * {@link QueryEngine} implementation that creates indexes.
 * 
 * @author Andreas Holstenson
 *
 */
public class Index
	implements QueryEngineFactory<IndexBuilder<?>, IndexConfig>
{
	@Override
	public String getId()
	{
		return "silo:index";
	}
	
	@Override
	public <P> IndexBuilder<?> builder(Function<QueryEngineConfig, P> configReceiver)
	{
		return new IndexQueryBuilderImpl<>(configReceiver);
	}
	
	@Override
	public Class<IndexConfig> getConfigClass()
	{
		return IndexConfig.class;
	}
	
	@Override
	public QueryEngine<?> create(QueryEngineCreationEncounter<IndexConfig> encounter)
	{
		Fields fields = encounter.getFields();
		IndexConfig config = encounter.getConfig();
		MVStoreManager store = encounter.openStorageWideMVStore(encounter.getName());
		return new IndexQueryEngine(encounter.getName(), fields, store, config);
	}
	

	/**
	 * Create a new {@link SearchIndexBuilder}. This method is used by
	 * entities when constructing indexes.
	 * 
	 * @param f
	 * @return
	 */
	public static <Parent> IndexBuilder<Parent> queryEngine(Function<QueryEngineConfig, Parent> configReceiver)
	{
		return new IndexQueryBuilderImpl<>(configReceiver);
	}
}
