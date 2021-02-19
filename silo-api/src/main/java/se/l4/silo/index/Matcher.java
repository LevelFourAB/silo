package se.l4.silo.index;

/**
 * Matcher abstraction used with certain {@link Query index queries}.
 */
public interface Matcher<V>
{
	static <V> Matcher<V> isEqualTo(V value)
	{
		return EqualsMatcher.create(value);
	}

	interface ComposableBuilder<ReturnPath, V>
	{
		/**
		 * Math using the given {@link Matcher}.
		 *
		 * @param matcher
		 * @return
		 *   previous builder
		 */
		ReturnPath matcher(Matcher<V> matcher);
	}
}
