package se.l4.silo.index;

import se.l4.silo.internal.NullMatcherImpl;

/**
 * Matcher that will only match if a value is {@code null}.
 */
public interface NullMatcher<V>
	extends Matcher<V>
{
	/**
	 * Create an instance of this matcher.
	 *
	 * @param <V>
	 * @return
	 */
	static <V> NullMatcher<V> create()
	{
		return NullMatcherImpl.create();
	}
}
