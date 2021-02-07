package se.l4.silo.engine.internal.collection;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import se.l4.silo.engine.collection.CountingCollector;

public class UnboundedSortedCountingCollector<V>
	implements CountingCollector<V>
{
	private final MutableObjectIntMap<V> data;

	public UnboundedSortedCountingCollector()
	{
		data = ObjectIntMaps.mutable.empty();
	}

	@Override
	public void offer(V item)
	{
		data.addToValue(item, 1);
	}

	@Override
	public void offer(V item, int count)
	{
		data.addToValue(item, count);
	}

	@Override
	public ListIterable<Entry<V>> withCounts()
	{
		MutableList<Entry<V>> items = Lists.mutable.empty();
		data.forEachKeyValue((key, value) -> items.add(new CountingCollectorEntry<>(key, value)));

		return items
			.sortThis((o1, o2) -> Integer.compare(o2.getCount(), o1.getCount()))
			.toImmutable();
	}

	@Override
	public ListIterable<V> items()
	{
		return withCounts().collect(i -> i.getItem());
	}
}
