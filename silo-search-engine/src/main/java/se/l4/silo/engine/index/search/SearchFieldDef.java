package se.l4.silo.engine.index.search;

import java.util.function.Function;

import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.index.search.internal.SearchFieldDefImpl;
import se.l4.silo.engine.index.search.types.SearchFieldType;

/**
 * Definition of a field that can be used in a {@link SearchIndexDef search index}.
 */
public interface SearchFieldDef<T>
{
	/**
	 * Get the name of the field.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get if the field is localized.
	 *
	 * @return
	 */
	boolean isLanguageSpecific();

	/**
	 * Get if this field should be highlighted.
	 *
	 * @return
	 */
	boolean isHighlighted();

	/**
	 * Get information about the type of the field.
	 *
	 * @return
	 */
	SearchFieldType<?> getType();

	/**
	 * Field representing a single value that may be queried.
	 */
	interface Single<T, V>
		extends SearchFieldDef<T>
	{
		/**
		 * Get a function that can be used to read this field from a certain
		 * object.
		 *
		 * @return
		 */
		Function<T, V> getSupplier();

		/**
		 * Get if this field is sorted.
		 *
		 * @return
		 */
		boolean isSorted();
	}

	/**
	 * Field containing multiple values that may be queried.
	 */
	interface Collection<T, V>
		extends SearchFieldDef<T>
	{
		/**
		 * Get a function that can be used to read this field from a certain
		 * object.
		 *
		 * @return
		 */
		Function<T, Iterable<V>> getSupplier();
	}

	/**
	 * Start building a new field definition.
	 *
	 * @param <T>
	 * @param type
	 *   the type that this field belongs to
	 * @param name
	 *   the name of the field
	 * @return
	 */
	static <T> Builder<T, Void> create(Class<T> type, String name)
	{
		return SearchFieldDefImpl.create(name, type);
	}

	interface Builder<T, F>
		extends BaseBuilder<Builder<T, F>>
	{
		/**
		 * Set the type of data this field contains.
		 *
		 * @param type
		 * @return
		 */
		<NF> Builder<T, NF> withType(SearchFieldType<NF> type);

		/**
		 * Set the type of data this field contains.
		 *
		 * @param buildable
		 * @return
		 */
		<NF> Builder<T, NF> withType(Buildable<? extends SearchFieldType<NF>> buildable);

		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		SingleBuilder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Indicate that this field can support multiple values.
		 *
		 * @return
		 */
		CollectionBuilder<T, F> collection();
	}

	interface BaseBuilder<Self extends BaseBuilder<Self>>
	{
		/**
		 * Set that this field should be indexed in a language specific way.
		 *
		 * @return
		 */
		default Self withLanguageSpecific()
		{
			return withLanguageSpecific(true);
		}

		/**
		 * Set that this field should be indexed in a language specific way.
		 *
		 * @return
		 */
		Self withLanguageSpecific(boolean languageSpecific);

		/**
		 * Indicate that the field can be highlighted.
		 *
		 * @return
		 */
		default Self withHighlighting()
		{
			return withHighlighting(true);
		}

		/**
		 * Indicate if the field should be able to highlight content.
		 *
		 * @param highlighted
		 * @return
		 */
		Self withHighlighting(boolean highlighted);
	}

	interface SingleBuilder<T, F>
		extends BaseBuilder<SingleBuilder<T, F>>, Buildable<SearchFieldDef.Single<T, F>>
	{
		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		SingleBuilder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Mark this field as being sortable.
		 *
		 * @return
		 */
		default SingleBuilder<T, F> sortable()
		{
			return withSortable(true);
		}

		/**
		 * Set if this field is sortable or not.
		 *
		 * @param sorted
		 * @return
		 */
		SingleBuilder<T, F> withSortable(boolean sorted);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		SearchFieldDef.Single<T, F> build();
	}

	interface CollectionBuilder<T, F>
		extends BaseBuilder<CollectionBuilder<T, F>>, Buildable<SearchFieldDef.Collection<T, F>>
	{
		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		CollectionBuilder<T, F> withSupplier(Function<T, Iterable<F>> supplier);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		SearchFieldDef.Collection<T, F> build();
	}
}
