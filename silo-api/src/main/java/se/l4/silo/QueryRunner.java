package se.l4.silo;

import java.util.function.Function;

/**
 * Receiver of query operations from a {@link Query}.
 * 
 * @author Andreas Holstenson
 *
 * @param <R>
 */
public interface QueryRunner<T, R>
{
	/**
	 * Query and fetch results matching.
	 * 
	 * @param data
	 * @return
	 */
	FetchResult<R> fetchResults(Object data, Function<QueryResult<T>, R> resultCreator);
}
