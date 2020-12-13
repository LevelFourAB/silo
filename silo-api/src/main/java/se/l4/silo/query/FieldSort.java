package se.l4.silo.query;

import se.l4.silo.internal.FieldSortImpl;

/**
 * Information about sorting on a field.
 */
public interface FieldSort
{
	/**
	 * Get the field being sorted on.
	 *
	 * @return
	 */
	String getField();

	/**
	 * Get if the sorting is to be done in ascending order.
	 *
	 * @return
	 */
	boolean isAscending();

	/**
	 * Create a new instance representing the given field and sort order.
	 *
	 * @param field
	 *   the field being sorted on
	 * @param isAscending
	 *   if the field is sorted in ascending or not
	 * @return
	 *   instance
	 */
	public static FieldSort create(String field, boolean isAscending)
	{
		return new FieldSortImpl(field, isAscending);
	}
}
