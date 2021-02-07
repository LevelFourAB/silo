package se.l4.silo.engine.collection;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.internal.collection.UnboundedSortedCountingCollector;

/**
 * Collector for collecting things with counts. Different implementations
 * provide different behavior for things such as the number of items they
 * return.
 */
public interface CountingCollector<V>
{
	/**
	 * Offer an item to the collector.
	 *
	 * @param item
	 */
	void offer(V item);

	/**
	 * Offer an item to the collector.
	 *
	 * @param item
	 * @param count
	 */
	void offer(V item, int count);

	/**
	 * Get the items with their counts.
	 *
	 * @return
	 */
	ListIterable<Entry<V>> withCounts();

	/**
	 * Get the items.
	 *
	 * @return
	 */
	ListIterable<V> items();

	/**
	 * Create an instance that is unbounded and will return all items.
	 *
	 * @param <V>
	 * @return
	 */
	static <V> CountingCollector<V> sorted()
	{
		return new UnboundedSortedCountingCollector<>();
	}

	/**
	 * Create an instance that collect the top K items.
	 *
	 * @param <V>
	 * @param k
	 * @return
	 */
	static <V> TopK<V> topK(int k)
	{
		return TopK.exact(k);
	}

	/**
	 * Entry containing information about an item with a count.
	 */
	interface Entry<V>
	{
		/**
		 * Get the item.
		 *
		 * @return
		 */
		V getItem();

		/**
		 * Get the count.
		 *
		 * @return
		 */
		int getCount();
	}
}
