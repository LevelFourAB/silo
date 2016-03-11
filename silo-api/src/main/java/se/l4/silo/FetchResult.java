package se.l4.silo;

import java.util.function.Function;

import com.google.common.collect.Iterators;

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
	 * Get the total number of results available.
	 * 
	 * @return
	 */
	int getTotal();

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
	
	/**
	 * Get a {@link FetchResult} representing the fetch of a single item.
	 * 
	 * @param data
	 * @return
	 */
	static <N> FetchResult<N> single(N data)
	{
		return new IteratorFetchResult<>(Iterators.singletonIterator(data), 1, 0, -1, 1);
	}
}
