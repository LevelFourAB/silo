package se.l4.silo.index;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.internal.FieldIndexQueryImpl;
import se.l4.silo.query.FieldLimit;
import se.l4.silo.query.FieldSort;
import se.l4.silo.query.FieldSortBuilder;
import se.l4.silo.query.LimitableQuery;
import se.l4.silo.query.Matcher;
import se.l4.silo.query.Query;

/**
 * Simple query abilities that run on top of indexed fields.
 *
 * @param <T>
 */
public interface FieldIndexQuery<T>
	extends Query<T, T, FieldIndexResult<T>>, LimitableQuery
{
	/**
	 * Get fields that are being limited.
	 *
	 * @return
	 */
	ListIterable<FieldLimit> getLimits();

	/**
	 * Get fields that are being sorted on.
	 *
	 * @return
	 */
	ListIterable<FieldSort> getSortOrder();

	/**
	 * Get if the default sort is ascending or not.
	 *
	 * @return
	 */
	boolean isAscendingDefaultSort();

	/**
	 * Start building a new query on field index.
	 *
	 * @param <T>
	 *   type to query
	 * @param index
	 *   name of the index being queried
	 * @param type
	 *   reference to the type being queried
	 * @return
	 *   builder for query
	 */
	static <T> Builder<T> create(String index, Class<T> type)
	{
		return FieldIndexQueryImpl.create(index);
	}

	/**
	 * Builder used to create queries on standard field indexes.
	 */
	interface Builder<T>
		extends LimitableQuery.Builder<Builder<T>>
	{
		/**
		 * Add a pre-built {@link FieldLimit} to this query.
		 *
		 * @param limit
		 * @return
		 */
		Builder<T> add(FieldLimit limit);

		/**
		 * Limit the given field in a fluent way.
		 *
		 * @param name
		 *   the name of the field
		 * @return
		 *   builder that can be used to define how the field is to be
		 *   matched
		 */
		FieldIndexLimitBuilder<Builder<T>, Object> field(String name);

		/**
		 * Limit the given field using a pre-built matcher.
		 *
		 * @param name
		 *   the name of the field
		 * @param matcher
		 *   the matcher that should be used to limit the field. Created
		 *   via static methods in {@link Matcher}
		 * @return
		 *   copy of builder with field limit added
		 */
		Builder<T> field(String name, Matcher matcher);

		/**
		 * Sort on the given field, specifying ascending or descending order
		 * in a fluent way.
		 *
		 * @param name
		 *   the name of the field
		 * @return
		 *   builder that can be used to define the sort order
		 */
		FieldSortBuilder<Builder<T>> sort(String name);

		/**
		 * Sort on the given field and direction.
		 *
		 * @param sort
		 * @return
		 */
		Builder<T> sort(FieldSort sort);

		/**
		 * Indicate the default sort should be reversed.
		 *
		 * @return
		 */
		Builder<T> defaultSort(boolean ascending);

		/**
		 * Build instance of {@link FieldIndexQuery}.
		 *
		 * @return
		 */
		FieldIndexQuery<T> build();
	}
}
