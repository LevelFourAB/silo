package se.l4.silo.index.search.query;

import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.internal.FieldQueryImpl;

/**
 * Query a specific field using a {@link Matcher}.
 */
public interface FieldQuery
	extends QueryClause
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
	 * Get boost of this query.
	 *
	 * @return
	 */
	float getBoost();

	/**
	 * Create a query for a field.
	 *
	 * @param field
	 *   the field being limited
	 * @param matcher
	 *   the matcher to use for limiting
	 * @return
	 *   new instance of {@link FieldLimit}
	 */
	static FieldQuery create(String field, Matcher matcher)
	{
		return new FieldQueryImpl(field, matcher, 1f);
	}
}
