package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.IndexBuilder;
import se.l4.silo.engine.config.IndexConfig;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.internal.index.IndexQueryBuilderImpl;
import se.l4.silo.engine.internal.index.IndexQueryEngine;

/**
 * Query engine that supports indexing and querying a set of fields.
 * 
 * @author Andreas Holstenson
 *
 */
public class IndexQueryEngineFactory
	implements QueryEngineFactory<IndexBuilder<?>, IndexConfig>
{
	private static final IndexQueryEngineFactory INSTANCE = new IndexQueryEngineFactory();
	
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
		MVStoreManager store = encounter.openMVStore(encounter.getName());
		return new IndexQueryEngine(encounter.getName(), fields, store, config);
	}

	/**
	 * Get an instance of this factory for use with builders.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> QueryEngineFactory<IndexBuilder<T>, ?> type()
	{
		return (QueryEngineFactory) INSTANCE;
	}
}
