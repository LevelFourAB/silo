package se.l4.silo;

/**
 * Receiver of query operations from a {@link Query}.
 * 
 * @author Andreas Holstenson
 *
 * @param <R>
 */
public interface QueryRunner<R>
{
	/**
	 * Query and fetch results matching.
	 * 
	 * @param data
	 * @return
	 */
	FetchResult<R> fetchResults(Object data);
}
