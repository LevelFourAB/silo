package se.l4.silo.engine;

import se.l4.silo.engine.types.FieldType;

/**
 * Information about a field.
 *
 * @author Andreas Holstenson
 *
 */
public interface FieldDef
{
	/**
	 * Get the name of this field.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the type of this field.
	 *
	 * @return
	 */
	FieldType<?> getType();

	/**
	 * Get if the field supports multiple values.
	 *
	 * @return
	 */
	boolean isCollection();
}
