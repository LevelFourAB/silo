package se.l4.silo.engine;

import java.util.Optional;

/**
 * Information about fields.
 *
 * @author Andreas Holstenson
 *
 */
public interface Fields
{
	/**
	 * Get a definition for the given field.
	 *
	 * @param name
	 * @return
	 */
	Optional<FieldDef> get(String name);
}
