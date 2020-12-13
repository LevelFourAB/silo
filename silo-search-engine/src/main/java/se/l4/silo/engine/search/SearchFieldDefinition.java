package se.l4.silo.engine.search;

import java.util.function.Function;

import se.l4.silo.engine.search.internal.FieldDefinitionImpl;

/**
 * Definition of a field that can be used in a {@link SearchIndexDefinition search index}.
 */
public interface SearchFieldDefinition<T>
{
	/**
	 * Get the name of the field.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get a function that can be used to read this field from a certain
	 * object.
	 *
	 * @return
	 */
	Function<T, Object> getSupplier();

	/**
	 * Get if the field is localized.
	 *
	 * @return
	 */
	boolean isLanguageSpecific();

	/**
	 * Get if the field is indexed.
	 *
	 * @return
	 */
	boolean isIndexed();

	/**
	 * Get if this field should be highlighted.
	 *
	 * @return
	 */
	boolean isHighlighted();

	/**
	 * Get if this field is sorted.
	 *
	 * @return
	 */
	boolean isSorted();

	/**
	 * Get information about the type of the field.
	 *
	 * @return
	 */
	SearchFieldType<?> getType();

	static <T> Builder<T, Void> create(String name, Class<T> type)
	{
		return FieldDefinitionImpl.create(name, type);
	}

	interface Builder<T, F>
	{
		/**
		 * Set the type of data this field contains.
		 *
		 * @param type
		 * @return
		 */
		<NF> Builder<T, NF> withType(SearchFieldType<NF> type);

		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		Builder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Indicate that this field can support multiple values.
		 *
		 * @return
		 */
		Builder<T, Iterable<F>> collection();

		/**
		 * Set that this field should be indexed in a language specific way.
		 *
		 * @return
		 */
		Builder<T, F> withLanguageSpecific(boolean languageSpecific);

		/**
		 * Indicate that the field can be highlighted.
		 *
		 * @return
		 */
		Builder<T, F> withHighlighting();

		/**
		 * Indicate if the field should be able to highlight content.
		 *
		 * @param highlighted
		 * @return
		 */
		Builder<T, F> withHighlighting(boolean highlighted);

		/**
		 * Mark this field as being sortable.
		 *
		 * @return
		 */
		Builder<T, F> sortable();

		/**
		 * Set if this field is sortable or not.
		 *
		 * @param sorted
		 * @return
		 */
		Builder<T, F> withSortable(boolean sorted);

		SearchFieldDefinition<T> build();
	}
}
