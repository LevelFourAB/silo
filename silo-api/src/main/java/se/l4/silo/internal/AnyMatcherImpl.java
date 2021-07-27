package se.l4.silo.internal;

import se.l4.silo.index.AnyMatcher;

/**
 * Implementation of {@link AnyMatcher}.
 */
public class AnyMatcherImpl<V>
	implements AnyMatcher<V>
{
	private static final AnyMatcher<Object> INSTANCE = new AnyMatcherImpl<>();

	private AnyMatcherImpl()
	{
	}

	@Override
	public String toString()
	{
		return "AnyMatcher{}";
	}

	/**
	 * Get an instance.
	 *
	 * @param <V>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <V> AnyMatcher<V> create()
	{
		return (AnyMatcher<V>) INSTANCE;
	}
}
