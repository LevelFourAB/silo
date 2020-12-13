package se.l4.silo.query;

import java.util.Objects;

import se.l4.silo.internal.EqualsMatcherImpl;

/**
 * {@link Matcher} describing that something should be equal to a certain
 * value.
 */
public interface EqualsMatcher
	extends Matcher
{
	/**
	 * Get the value to be matched against.
	 */
	Object getValue();

	/**
	 * Create a matcher for the given value.
	 *
	 * @param value
	 * @return
	 */
	static EqualsMatcher create(Object value)
	{
		Objects.requireNonNull(value);
		return new EqualsMatcherImpl(value);
	}

	/**
	 * Builder for limiting something based on equality.
	 */
	interface ComposableBuilder<ReturnPath, V>
		extends Matcher.ComposableBuilder<ReturnPath>
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
