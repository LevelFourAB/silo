package se.l4.silo.engine;

import se.l4.silo.engine.builder.SearchEngineBuilder;
import se.l4.silo.engine.builder.SearchIndexBuilder;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.internal.search.SearchIndexBuilderImpl;
import se.l4.silo.engine.internal.search.SearchIndexQueryEngine;
import se.l4.silo.engine.search.internal.SearchEngineBuilderImpl;

/**
 * Utilities to access creation of {@link SearchIndexQueryEngine}s.
 * 
 * @author Andreas Holstenson
 *
 */
public class SearchIndex
{
	private SearchIndex()
	{
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
	 * 
	 * @return
	 */
	public static <T> QueryEngineBuilderFactory<T, SearchIndexBuilder<T>> engine()
	{
		return (c) -> new SearchIndexBuilderImpl<T>(c);
	}
}
