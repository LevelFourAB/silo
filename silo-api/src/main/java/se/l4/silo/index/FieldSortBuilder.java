package se.l4.silo.index;

import java.util.function.Function;

import se.l4.silo.internal.FieldSortBuilderImpl;

/**
 * Builder for specifying sorting in a fluent API.
 */
public interface FieldSortBuilder<ReturnPath>
{
	/**
	 * Sort ascending order.
	 *
	 * @return
	 */
	ReturnPath sortAscending();

	/**
	 * Sort in descending order.
	 *
	 * @return
	 */
	ReturnPath sortDescending();

	/**
	 * Sort by the given order.
	 *
	 * @param ascending
	 * @return
	 */
	ReturnPath sort(boolean ascending);

	/**
	 * Create a new builder that will pass an instance of {@link FieldSort}
	 * to the specified receiver.
	 *
	 * @param <ReturnPath>
	 *   type that should be returned when sort has finished building
	 * @param field
	 *   the field being sorted on
	 * @param resultReceiver
	 *   function used to receive {@link FieldSort} and return a new instance
	 *   of {@code <ReturnPath>}
	 * @return
	 *   builder
	 */
	static <ReturnPath> FieldSortBuilder<ReturnPath> create(
		String field,
		Function<FieldSort, ReturnPath> resultReceiver
	)
	{
		return new FieldSortBuilderImpl<>(field, resultReceiver);
	}
}
