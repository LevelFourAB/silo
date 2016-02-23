package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.IndexBuilder;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.internal.index.IndexQueryBuilderImpl;

/**
 * Query engine that supports indexing and querying a set of fields.
 * 
 * @author Andreas Holstenson
 *
 */
public class IndexQueryEngine
	implements QueryEngineFactory<IndexBuilder<?>>
{
	private static final IndexQueryEngine INSTANCE = new IndexQueryEngine();
	
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
	public QueryEngine<?> create(QueryEngineConfig config)
	{
		return new se.l4.silo.engine.internal.query.IndexQueryEngine<>();
	}

	/**
	 * Get an instance of this factory for use with builders.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> QueryEngineFactory<IndexBuilder<T>> type()
	{
		return (QueryEngineFactory) INSTANCE;
	}
}
