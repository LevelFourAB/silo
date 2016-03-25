package se.l4.silo.search;

public interface SearchHit<T>
{
	/**
	 * Get the matching data for this hit.
	 * 
	 * @return
	 */
	T item();
}
