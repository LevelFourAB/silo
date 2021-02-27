package se.l4.silo.engine.index.search;

import java.util.Locale;
import java.util.function.Function;

import org.eclipse.collections.api.RichIterable;

import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.silo.engine.index.search.facets.FacetDef;
import se.l4.silo.engine.index.search.internal.SearchIndexDefinitionImpl;
import se.l4.silo.engine.index.search.locales.Locales;

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

	/**
	 * Get the facets of this index.
	 *
	 * @return
	 */
	RichIterable<FacetDef<T, ?, ?>> getFacets();

	/**
	 * Create an index for the given type.
	 *
	 * @param <T>
	 *   type
	 * @param type
	 *   type the index is for
	 * @param name
	 *   the name of the index
	 * @return
	 */
	static <T> Builder<T> create(Class<T> type, String name)
	{
		return SearchIndexDefinitionImpl.create(type, name);
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
		 * Add a field to this configuration.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<T> addField(Buildable<? extends SearchFieldDefinition<T>> buildable);

		/**
		 * Add multiple fields to this configuration.
		 *
		 * @param fields
		 * @return
		 */
		Builder<T> addFields(Iterable<? extends SearchFieldDefinition<T>> fields);

		/**
		 * Add a facet to this definition.
		 *
		 * @param facet
		 * @return
		 */
		Builder<T> addFacet(FacetDef<T, ?, ?> facet);

		/**
		 * Add multiple facets to this configuration.
		 *
		 * @param facets
		 * @return
		 */
		Builder<T> addFacets(Iterable<? extends FacetDef<T, ?, ?>> facets);

		/**
		 * Build the index definition.
		 */
		SearchIndexDefinition<T> build();
	}
}
