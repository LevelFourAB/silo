package se.l4.silo;

import java.util.function.Function;

import se.l4.silo.raw.TransformingFetchResult;

public interface FetchResult<T>
	extends AutoCloseable, Iterable<T>
{
	/**
	 * Get the number of returned results.
	 * 
	 * @return
	 */
	int getSize();
	
	/**
	 * Get the offset used to fetch these results.
	 * 
	 * @return
	 */
	int getOffset();
	
	/**
	 * Get the limit used to fetch these results.
	 * 
	 * @return
	 */
	int getLimit();

	/**
	 * Check if these results are empty.
	 * 
	 * @return
	 */
	boolean isEmpty();
	
	@Override
	void close();
	
	default <N> FetchResult<N> transform(Function<T, N> func)
	{
		return new TransformingFetchResult<N>(this, func);
	}
	
	/**
	 * Get an empty fetch result.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static <N> FetchResult<N> empty()
	{
		return (FetchResult<N>) EmptyFetchResult.INSTANCE;
	}
}
