package se.l4.silo.engine.search;

import java.util.Locale;

import org.apache.lucene.index.IndexableField;

import se.l4.silo.engine.search.internal.FieldDefinitionImpl;
import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;

/**
 * Definition of a field in a {@link SearchIndexQueryEngine}.
 *
 * @author Andreas Holstenson
 *
 */
public interface FieldDefinition
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
	 * Get if the field is indexed.
	 *
	 * @return
	 */
	boolean isIndexed();

	/**
	 * Get if this field should be stored.
	 *
	 * @return
	 */
	boolean isStored();

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
	 * Get if this field should also store values.
	 *
	 * @return
	 */
	boolean isStoreValues();

	/**
	 * Get under which name this field is stored.
	 *
	 * @param language
	 * @return
	 */
	String name(Locale language);

	default String name(Language language)
	{
		return name(language.getLocale());
	}

	/**
	 * Get under which names a doc values field is stored.
	 *
	 * @param language
	 * @return
	 */
	String docValuesName(Locale language);

	/**
	 * Get under which names a doc values field is stored.
	 *
	 * @param language
	 * @return
	 */
	String sortValuesName(Locale language);

	/**
	 * Get under which name this field is stored for the specific language.
	 *
	 * @param field
	 * @param language
	 * @return
	 */
	String name(String field, Language language);

	/**
	 * Get under which name this field is stored for the specific language.
	 *
	 * @param field
	 * @param language
	 * @return
	 */
	String name(String field, Locale language);

	default String nullName()
	{
		return nullName(getName());
	}

	/**
	 * Get the name of this field when stored a {@code null} value.
	 *
	 * @param field
	 * @return
	 */
	String nullName(String field);

	/**
	 * Get information about the type of the field.
	 *
	 * @return
	 */
	SearchFieldType getType();

	/**
	 * Create a new {@link IndexableField}.
	 *
	 * @param name
	 * @param language
	 * @param data
	 * @return
	 */
	IndexableField createIndexableField(String name, Language language, Object data);

	/**
	 * Create a {@link IndexableField} for storing values for retrieval during
	 * searching and scoring.
	 *
	 * @param name
	 * @param language
	 * @param data
	 * @return
	 */
	IndexableField createValuesField(String name, Language language, Object data);

	/**
	 * Create a {@link IndexableField} for storing values for sorting.
	 *
	 * @param name
	 * @param language
	 * @param data
	 * @return
	 */
	IndexableField createSortingField(String name, Language language, Object data);

	static Builder builder()
	{
		return new FieldDefinitionImpl.BuilderImpl();
	}

	interface Builder
	{
		Builder setName(String name);

		Builder setType(SearchFieldType type);

		Builder setLanguageSpecific(boolean languageSpecific);

		Builder setSorted(boolean sorted);

		FieldDefinition build();
	}
}
