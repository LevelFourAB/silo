package se.l4.silo.engine.search;

/**
 * Encounter for adding custom fields to the search index.
 * 
 * @author Andreas Holstenson
 *
 */
public interface FieldCreationEncounter
{
	/**
	 * Add a field to the index. The field must have been previously
	 * defined.
	 * 
	 * @param name
	 * @param value
	 */
	void add(String name, Object value);
	
	/**
	 * Add a field of a specific type to the index.
	 * 
	 * @param name
	 * @param value
	 * @param fieldType
	 * @return
	 */
	IndexedFieldBuilder add(String name, Object value, SearchFieldType fieldType);
}
