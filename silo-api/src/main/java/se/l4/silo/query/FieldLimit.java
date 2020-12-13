package se.l4.silo.query;

import se.l4.silo.internal.FieldLimitImpl;

/**
 * Limit as commonly used in {@link Query index queries}.
 */
public interface FieldLimit
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
	Matcher getMatcher();

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
	static FieldLimit create(String field, Matcher matcher)
	{
		return new FieldLimitImpl(field, matcher);
	}
}
