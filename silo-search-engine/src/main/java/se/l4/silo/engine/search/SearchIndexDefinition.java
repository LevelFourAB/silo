package se.l4.silo.engine.search;

import java.util.Locale;
import java.util.function.Function;

import org.eclipse.collections.api.RichIterable;

import se.l4.silo.engine.IndexDefinition;
import se.l4.silo.engine.search.internal.SearchIndexDefinitionImpl;

/**
 * Definition that activates support for creating indexes that support
 * advanced querying via a search index.
 */
public interface SearchIndexDefinition<T>
	extends IndexDefinition<T>
{
	/**
	 * Get function used to extract the locale of a stored object.
	 *
	 * @return
	 */
	Function<T, Locale> getLocaleSupplier();

	/**
	 * Get the fields that this engine will index.
	 *
	 * @return
	 */
	RichIterable<SearchFieldDefinition<T>> getFields();

	static <T> Builder<T> create(String name, Class<T> type)
	{
		return SearchIndexDefinitionImpl.create(name, type);
	}

	interface Builder<T>
	{
		/**
		 * Set the instance of {@link Locales} to use.
		 *
		 * @param locales
		 * @return
		 */
		Builder<T> withLocales(Locales locales);

		/**
		 * Set the supplier used to determine what locale an object should
		 * be indexed as.
		 *
		 * @param supplier
		 * @return
		 */
		Builder<T> withLocaleSupplier(Function<T, Locale> supplier);

		/**
		 * Set a static locale for this index.
		 *
		 * @param locale
		 * @return
		 */
		Builder<T> withLocale(Locale locale);

		/**
		 * Add a field to this configuration.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addField(SearchFieldDefinition<T> field);

		/**
		 * Build the index definition.
		 */
		SearchIndexDefinition<T> build();
	}
}