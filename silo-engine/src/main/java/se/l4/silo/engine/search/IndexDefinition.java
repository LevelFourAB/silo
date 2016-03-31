package se.l4.silo.engine.search;

import java.util.Locale;
import java.util.Set;

import se.l4.silo.engine.internal.search.SearchIndexQueryEngine;

/**
 * Definition used for a single {@link SearchIndexQueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface IndexDefinition
{
	/**
	 * Get the default language of the index.
	 * 
	 * @return
	 */
	Locale getDefaultLanguage();
	
	/**
	 * Get the field name that is used to figure out the language of the
	 * information being indexed.
	 * 
	 * @return
	 */
	String getLanguageField();
	
	/**
	 * Retrieve a field from the definition.
	 * 
	 * @param name
	 * @return
	 */
	FieldDefinition getField(String name);
	
	/**
	 * Retrieve a field based on the name used internally in the search engine.
	 * 
	 * @param name
	 * @return
	 */
	FieldDefinition getFieldFromIndexName(String name);
	
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
	Iterable<FieldDefinition> getFields();
	
	/**
	 * Get a facet based on its identifier.
	 * 
	 * @param facetId
	 * @return
	 */
	FacetDefinition getFacet(String facetId);

	/**
	 * Get all fields that will store values.
	 * 
	 * @see SearchFieldType#createValuesField(String, Language, Object)
	 * @return
	 */
	Set<String> getValueFields();
}
