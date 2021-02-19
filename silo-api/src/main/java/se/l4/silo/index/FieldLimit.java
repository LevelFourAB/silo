package se.l4.silo.index;

import se.l4.silo.internal.FieldLimitImpl;

/**
 * Limit as commonly used in {@link Query index queries}.
 */
public interface FieldLimit<V>
{
	/**
	 * Get the field being limited.
	 *
	 * @return
	 *   the name of the field this limit is for
	 */
	String getField();

	/**
	 * Get the matcher for the field.
	 *
	 * @return
	 *   the matcher being used to limit the field
	 */
	Matcher<V> getMatcher();

	/**
	 * Create a limit for a field.
	 *
	 * @param field
	 *   the field being limited
	 * @param matcher
	 *   the matcher to use for limiting
	 * @return
	 *   new instance of {@link FieldLimit}
	 */
	static <V> FieldLimit<V> create(String field, Matcher<V> matcher)
	{
		return new FieldLimitImpl<>(field, matcher);
	}
}
