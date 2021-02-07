package se.l4.silo.engine.internal.collection;

import java.util.PriorityQueue;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import se.l4.silo.engine.collection.TopK;

/**
 * Implementation of {@link TopK} backed by a map instance.
 */
public class MapBackedTopK<V>
	implements TopK<V>
{
	private final int k;
	private final MutableObjectIntMap<V> data;

	public MapBackedTopK(int k)
	{
		this.k = k;
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
		PriorityQueue<Entry<V>> queue = new PriorityQueue<>(
			k,
			(o1, o2) -> Integer.compare(o2.getCount(), o1.getCount())
		);

		data.forEachKeyValue((key, value) -> {
			if(queue.size() < k)
			{
				queue.add(new CountingCollectorEntry<>(key, value));
			}
			else if(value > queue.peek().getCount())
			{
				queue.add(new CountingCollectorEntry<>(key, value));
				queue.poll();
			}
		});

		MutableList<Entry<V>> items = Lists.mutable.empty();
		while(! queue.isEmpty())
		{
			items.add(queue.poll());
		}

		return items.toImmutable();
	}

	@Override
	public ListIterable<V> items()
	{
		return withCounts().collect(i -> i.getItem());
	}
}
