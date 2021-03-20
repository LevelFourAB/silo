package se.l4.silo.engine.internal.collection;

import java.util.function.Consumer;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import se.l4.silo.engine.collection.LongIntervalCollector;

/**
 * Implementation of {@link LongIntervalCollector} that simply loops over
 * all intervals.
 */
public class SimpleLongIntervalCollector<V>
	implements LongIntervalCollector<V>
{
	private final Entry<V>[] entries;

	@SuppressWarnings("unchecked")
	private SimpleLongIntervalCollector(
		RichIterable<Entry<V>> entries
	)
	{
		this.entries = entries.toArray(new Entry[entries.size()]);
	}

	@Override
	public void match(long value, Consumer<V> consumer)
	{
		for(Entry<V> entry : entries)
		{
			if(entry.start <= value && entry.end > value)
			{
				consumer.accept(entry.value);
			}
		}
	}

	public static <V> Builder<V> create()
	{
		return new BuilderImpl<>();
	}

	public static class BuilderImpl<V>
		implements Builder<V>
	{
		private final MutableList<Entry<V>> entries;

		public BuilderImpl()
		{
			entries = Lists.mutable.empty();
		}

		@Override
		public Builder<V> add(V value, long lower, long upper)
		{
			entries.add(new Entry<>(lower, upper, value));

			return this;
		}

		@Override
		public LongIntervalCollector<V> build()
		{
			return new SimpleLongIntervalCollector<>(entries);
		}
	}

	private static class Entry<V>
	{
		private final long start;
		private final long end;
		private final V value;

		public Entry(
			long start,
			long end,
			V value
		)
		{
			this.start = start;
			this.end = end;
			this.value = value;
		}
	}
}
