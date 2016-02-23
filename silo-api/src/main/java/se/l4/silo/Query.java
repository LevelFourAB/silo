package se.l4.silo;

/**
 * Query builder.
 * 
 * @author Andreas Holstenson
 *
 * @param <R>
 */
public interface Query<R>
{
	/**
	 * Run this query and return the result.
	 * 
	 * @return
	 */
	FetchResult<R> run();
}
