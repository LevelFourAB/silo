package se.l4.silo.query;

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
	R run();
}
