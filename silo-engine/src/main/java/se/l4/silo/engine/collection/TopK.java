package se.l4.silo.engine.collection;

import se.l4.silo.engine.internal.collection.MapBackedTopK;

/**
 * Interface for Top-K summary operations.
 */
public interface TopK<V>
	extends CountingCollector<V>
{
	/**
	 * Create an instance that can be used for exact counts.
	 *
	 * @param <V>
	 * @param k
	 * @return
	 */
	static <V> TopK<V> exact(int k)
	{
		return new MapBackedTopK<>(k);
	}
}
