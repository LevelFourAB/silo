package se.l4.silo.index;

import java.util.Objects;

import se.l4.silo.internal.EqualsMatcherImpl;

/**
 * {@link Matcher} describing that something should be equal to a certain
 * value.
 */
public interface EqualsMatcher<V>
	extends Matcher<V>, MappableMatcher<V>
{
	/**
	 * Get the value to be matched against.
	 */
	V getValue();

	/**
	 * Create a matcher for the given value.
	 *
	 * @param value
	 * @return
	 */
	static <V> EqualsMatcher<V> create(V value)
	{
		Objects.requireNonNull(value);
		return new EqualsMatcherImpl<>(value);
	}

	/**
	 * Builder for limiting something based on equality.
	 */
	interface ComposableBuilder<ReturnPath, V>
		extends Matcher.ComposableBuilder<ReturnPath, V>
	{
		/**
		 * Match field against a specific value.
		 *
		 * @param value
		 * @return
		 */
		default ReturnPath isEqualTo(V value)
		{
			return matcher(EqualsMatcher.create(value));
		}
	}
}
