package se.l4.silo.engine.builder;

import org.apache.bval.jsr303.xml.FieldType;

public interface FieldLimited<T>
{
	/**
	 * Set the limit for a specific field.
	 * 
	 * @param field
	 * 		name of the field
	 * @param type
	 * 		type to limit to
	 * @return
	 */
	T setFieldLimit(String field, FieldType type);
	
	/**
	 * Set the limit for a specific field.
	 * 
	 * @param field
	 * 		name of the field
	 * @param type
	 * 		type to limit to
	 * @param length
	 * 		the length to limit to
	 * @return
	 */
	T setFieldLimit(String field, FieldType type, int length);
	
	/**
	 * Set the limit for the specified field.
	 * 
	 * @param field
	 * @return
	 */
	FieldLimitBuilder<T> setFieldLimit(String field);
}
