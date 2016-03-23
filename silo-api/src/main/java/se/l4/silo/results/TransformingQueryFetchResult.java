package se.l4.silo.results;

import java.util.function.Function;

import se.l4.silo.query.QueryFetchResult;

/**
 * Implementation of {@link QueryFetchResult} for transforming results.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class TransformingQueryFetchResult<T>
	extends TransformingFetchResult<T>
	implements QueryFetchResult<T>
{
	private final QueryFetchResult<?> in;

	public <I> TransformingQueryFetchResult(QueryFetchResult<I> in, Function<I, T> func)
	{
		super(in, func);
		
		this.in = in;
	}

	@Override
	public <M> M getMetadata(String key)
	{
		return in.getMetadata(key);
	}
}
