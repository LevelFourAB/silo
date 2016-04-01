package se.l4.silo.engine.search;

import java.util.Locale;

import org.apache.lucene.index.IndexableField;

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
	 * Get if the field will allow multiple values.
	 * 
	 * @return
	 */
	boolean isMultiValued();
	
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
	 * Get under which name this field is stored.
	 * 
	 * @param language
	 * @return
	 */
	String name(Locale language);
	
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
}
