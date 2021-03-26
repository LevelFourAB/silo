package se.l4.silo.engine.index.search;

import org.eclipse.collections.api.RichIterable;

import se.l4.silo.engine.index.search.locales.LocaleSupport;

/**
 * Encounter used to expose access to information about fields, names and
 * how data is structured in Lucene. Used to implement things like facets and
 * custom scoring.
 */
public interface SearchIndexEncounter<T>
{
	/**
	 * Retrieve a field from the definition.
	 *
	 * @param name
	 * @return
	 */
	SearchField<T, ?> getField(String name);

	/**
	 * Retrieve a field based on the name used internally in the search engine.
	 *
	 * @param name
	 * @return
	 */
	SearchField<T, ?> getFieldFromIndexName(String name);

	/**
	 * Resolve the regular name from the one used internally in the index.
	 *
	 * @param name
	 * @return
	 */
	String getNameFromIndexName(String name);

	/**
	 * Get all of the fields defined.
	 *
	 * @return
	 */
	RichIterable<SearchField<T, ?>> getFields();

	/**
	 * Get under which names a doc values field is stored.
	 *
	 * @param localeSupport
	 * @return
	 */
	String docValuesName(SearchFieldDef<?> field, LocaleSupport localeSupport);

	/**
	 * Get under which names a sort values field is stored.
	 *
	 * @param localeSupport
	 * @return
	 */
	String sortValuesName(SearchFieldDef<?> field, LocaleSupport localeSupport);

	/**
	 * Get under which name this field is stored for the specific language.
	 *
	 * @param field
	 * @param localeSupport
	 * @return
	 */
	String name(SearchFieldDef<?> field, LocaleSupport localeSupport);

	/**
	 * Get the name of the given field when stored as a {@code null} value.
	 *
	 * @param def
	 * @return
	 */
	String nullName(SearchFieldDef<?> def);
}
