package se.l4.silo.index;

import se.l4.silo.internal.AnyMatcherImpl;

/**
 * Matcher that matches any value. This can be used to match if a field exists
 * in the index.
 */
public interface AnyMatcher<V>
	extends Matcher<V>
{
	/**
	 * Create an instance of this matcher.
	 *
	 * @param <V>
	 * @return
	 */
	static <V> AnyMatcher<V> create()
	{
		return AnyMatcherImpl.create();
	}
}
