package se.l4.silo.query;

import java.util.function.Function;

import se.l4.silo.FetchResult;
import se.l4.silo.results.TransformingQueryFetchResult;

/**
 * Extension to {@link FetchResult} to support sending extra metadata in
 * the result.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface QueryFetchResult<T>
	extends FetchResult<T>
{
	/**
	 * Get some metadata from this query result.
	 * 
	 * @param key
	 * @return
	 */
	<M> M getMetadata(String key);
	
	/**
	 * Transform this fetch result using the given function.
	 * 
	 * @param func
	 * @return
	 */
	@Override
	default <N> QueryFetchResult<N> transform(Function<T, N> func)
	{
		return new TransformingQueryFetchResult<N>(this, func);
	}
}
