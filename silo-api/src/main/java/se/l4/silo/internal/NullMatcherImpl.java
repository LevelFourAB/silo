package se.l4.silo.internal;

import se.l4.silo.index.NullMatcher;

/**
 * Implementation of {@link NullMatcher}.
 */
public class NullMatcherImpl<V>
	implements NullMatcher<V>
{
	private static final NullMatcher<Object> INSTANCE = new NullMatcherImpl<>();

	private NullMatcherImpl()
	{
	}

	@Override
	public String toString()
	{
		return "NullMatcher{}";
	}

	/**
	 * Get an instance.
	 *
	 * @param <V>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <V> NullMatcher<V> create()
	{
		return (NullMatcher<V>) INSTANCE;
	}
}
