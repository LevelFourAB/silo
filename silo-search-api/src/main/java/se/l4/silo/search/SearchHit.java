package se.l4.silo.search;

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
}
