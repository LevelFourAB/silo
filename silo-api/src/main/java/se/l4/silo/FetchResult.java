package se.l4.silo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

import se.l4.silo.results.EmptyFetchResult;
import se.l4.silo.results.FetchResultSpliterator;
import se.l4.silo.results.IteratorFetchResult;
import se.l4.silo.results.TransformingFetchResult;

/**
 * Result of a fetch request, contains the results and information about the
 * parameters used for fetching.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
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
	
	/**
	 * Get the first entry is this result.
	 *  
	 * @return
	 */
	default Optional<T> first()
	{
		Iterator<T> it = iterator();
		if(it.hasNext())
		{
			return Optional.of(it.next());
		}
		
		return Optional.empty();
	}
	
	@Override
	void close();
	
	/**
	 * Transform this fetch result using the given function.
	 * 
	 * @param func
	 * @return
	 */
	default <N> FetchResult<N> transform(Function<T, N> func)
	{
		return new TransformingFetchResult<N>(this, func);
	}
	
	@Override
	default Spliterator<T> spliterator()
	{
		return new FetchResultSpliterator<T>(this);
	}
	
	/**
	 * Get a {@link Stream} for this result, see {@link Collection#stream()}
	 * for details. The returned stream will close this result when it itself
	 * is closed.
	 * 
	 * @return
	 */
    default Stream<T> stream()
    {
        Stream<T> stream = StreamSupport.stream(spliterator(), false);
        stream.onClose(this::close);
        return stream;
    }

	/**
	 * Get a {@link Stream} for this result for parallel operations,
	 * see {@link Collection#parallelStream()} for details.
	 * The returned stream will close this result when it itself is closed.
	 * 
	 * @return
	 */
    default Stream<T> parallelStream()
    {
        return StreamSupport.stream(spliterator(), true);
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
	
	/**
	 * Get a {@link FetchResult} for the given collection. This will assume
	 * that the offset and limit used to fetch this was zero.
	 * 
	 * @param collection
	 * @return
	 */
	static <N> FetchResult<N> forCollection(Collection<N> collection)
	{
		return new IteratorFetchResult<>(collection, 0, 0, collection.size());
	}
	
	/**
	 * Get a {@link FetchResult} for the given collection. This will assume
	 * that the offset and limit used to fetch this was zero.
	 * 
	 * @param collection
	 * @return
	 */
	static <N> FetchResult<N> forCollection(Collection<N> collection, int offset, int limit, int hits)
	{
		if(hits < collection.size())
		{
			throw new IllegalArgumentException("hits must be larger than or equal to collection size (" + hits + " < " + collection.size() + ")");
		}
		
		return new IteratorFetchResult<>(collection, offset, limit, hits);
	}
}
