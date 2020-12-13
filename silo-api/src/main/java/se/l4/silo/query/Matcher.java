package se.l4.silo.query;

/**
 * Matcher abstraction used with certain {@link Query index queries}.
 */
public interface Matcher
{
	static Matcher isEqualTo(Object value)
	{
		return EqualsMatcher.create(value);
	}

	interface ComposableBuilder<ReturnPath>
	{
		/**
		 * Math using the given {@link Matcher}.
		 *
		 * @param matcher
		 * @return
		 *   previous builder
		 */
		ReturnPath matcher(Matcher matcher);
	}
}
