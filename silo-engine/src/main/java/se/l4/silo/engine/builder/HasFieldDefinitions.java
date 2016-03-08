package se.l4.silo.engine.builder;

/**
 * Marker for builders that can be treated as structured data where different
 * fields may have different types.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface HasFieldDefinitions<T>
{
	/**
	 * Set the limit for a specific field.
	 * 
	 * @param field
	 * @param type
	 * @return
	 */
	T defineField(String field, String type);
	
	/**
	 * Set the limit for the specified field.
	 * 
	 * @param field
	 * @return
	 */
	FieldDefBuilder<T> defineField(String field);
}
