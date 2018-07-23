package se.l4.silo.search;

import java.util.function.Function;

public interface SearchHit<T>
{
	/**
	 * Get the score of this hit.
	 *
	 * @return
	 */
	float score();

	/**
	 * Get the matching data for this hit.
	 *
	 * @return
	 */
	T item();

	default <R> SearchHit<R> transform(Function<T, R> func)
	{
		return new SearchHitImpl<>(func.apply(item()), score());
	}
}
