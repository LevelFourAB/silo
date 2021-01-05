package se.l4.silo.engine.index.search;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;

import se.l4.silo.engine.index.search.types.SearchFieldType;

/**
 * Encounter used to expose access to information about fields, names and
 * how data is structured in Lucene. Used to implement things like facets and
 * custom scoring.
 */
public interface SearchIndexEncounter
{
	/**
	 * Retrieve a field from the definition.
	 *
	 * @param name
	 * @return
	 */
	SearchFieldDefinition<?> getField(String name);

	/**
	 * Retrieve a field based on the name used internally in the search engine.
	 *
	 * @param name
	 * @return
	 */
	SearchFieldDefinition<?> getFieldFromIndexName(String name);

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
	RichIterable<SearchFieldDefinition<?>> getFields();

	/**
	 * Get all fields that will store values.
	 *
	 * @see SearchFieldType#createValuesField(String, LocaleSupport, Object)
	 * @return
	 */
	SetIterable<String> getValueFields();

	/**
	 * Get under which names a doc values field is stored.
	 *
	 * @param localeSupport
	 * @return
	 */
	String docValuesName(SearchFieldDefinition<?> field, LocaleSupport localeSupport);

	/**
	 * Get under which names a sort values field is stored.
	 *
	 * @param localeSupport
	 * @return
	 */
	String sortValuesName(SearchFieldDefinition<?> field, LocaleSupport localeSupport);

	/**
	 * Get under which name this field is stored for the specific language.
	 *
	 * @param field
	 * @param localeSupport
	 * @return
	 */
	String name(SearchFieldDefinition<?> field, LocaleSupport localeSupport);

	/**
	 * Get the name of the given field when stored as a {@code null} value.
	 *
	 * @param def
	 * @return
	 */
	String nullName(SearchFieldDefinition<?> def);
}
