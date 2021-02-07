package se.l4.silo.engine.internal.collection;

import se.l4.silo.engine.collection.CountingCollector;

/**
 * Implementation of {@link CountingCollector.Entry}.
 */
public class CountingCollectorEntry<V>
	implements CountingCollector.Entry<V>
{
	private final V item;
	private final int count;

	public CountingCollectorEntry(V item, int count)
	{
		this.item = item;
		this.count = count;
	}

	@Override
	public V getItem()
	{
		return item;
	}

	@Override
	public int getCount()
	{
		return count;
	}

	@Override
	public String toString()
	{
		return "CountingCollector.Entry{item=" + item + ", count=" + count + "}";
	}
}
