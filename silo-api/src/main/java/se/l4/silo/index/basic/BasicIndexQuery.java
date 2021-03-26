package se.l4.silo.index.basic;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.FieldSort;
import se.l4.silo.index.FieldSortBuilder;
import se.l4.silo.index.LimitableQuery;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.Query;
import se.l4.silo.internal.BasicIndexQueryImpl;

/**
 * Simple query abilities that run on top of indexed fields.
 *
 * @param <T>
 */
public interface BasicIndexQuery<T>
	extends Query<T, T, BasicIndexResult<T>>, LimitableQuery
{
	/**
	 * Get fields that are being limited.
	 *
	 * @return
	 */
	ListIterable<FieldLimit<?>> getLimits();

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
		return BasicIndexQueryImpl.create(index);
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
		Builder<T> add(FieldLimit<?> limit);

		/**
		 * Limit the given field in a fluent way.
		 *
		 * @param name
		 *   the name of the field
		 * @return
		 *   builder that can be used to define how the field is to be
		 *   matched
		 */
		BasicFieldLimitBuilder<Builder<T>, Object> field(String name);

		/**
		 * Limit the given field in a fluent way.
		 *
		 * @param field
		 *   the field to limit
		 * @return
		 *   builder that can be used to define how the field is to be
		 *   matched
		 */
		<V> BasicFieldLimitBuilder<Builder<T>, V> field(BasicFieldRef<V> field);

		/**
		 * Limit the given field using a pre-built matcher.
		 *
		 * @param name
		 *   the name of the field
		 * @param matcher
		 *   the matcher that should be used to limit the field
		 * @return
		 *   copy of builder with field limit added
		 */
		Builder<T> field(String name, Matcher<?> matcher);

		/**
		 * Limit the given field using a pre-built matcher.
		 *
		 * @param field
		 *   the field to limit
		 * @param matcher
		 *   the matcher that should be used to limit the field
		 * @return
		 *   copy of builder with field limit added
		 */
		<V> Builder<T> field(BasicFieldRef<V> field, Matcher<V> matcher);

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
		 * Build instance of {@link BasicIndexQuery}.
		 *
		 * @return
		 */
		BasicIndexQuery<T> build();
	}
}
