package se.l4.silo.engine.collection;

import java.util.function.Consumer;

import se.l4.silo.engine.internal.collection.SimpleLongIntervalCollector;

/**
 * Helper for matching what intervals a long value falls within.
 */
public interface LongIntervalCollector<D>
{
	/**
	 * Match the given value and return all of the values it is associated
	 * with.
	 *
	 * @param value
	 * @param consumer
	 */
	void match(long value, Consumer<D> consumer);

	/**
	 * Start building a new collector.
	 *
	 * @param <D>
	 * @return
	 */
	static <D> Builder<D> create()
	{
		return SimpleLongIntervalCollector.create();
	}

	interface Builder<D>
	{
		/**
		 * Add an interval to the builder.
		 *
		 * @param value
		 *   value
		 * @param lower
		 *   lower value (inclusive)
		 * @param upper
		 *   upper value (exclusive)
		 * @return
		 */
		Builder<D> add(D value, long lower, long upper);

		/**
		 * Build the matcher.
		 *
		 * @return
		 */
		LongIntervalCollector<D> build();
	}
}
