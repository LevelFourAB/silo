package se.l4.silo.engine.builder;

import se.l4.silo.Silo;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.structured.StructuredEntity;

/**
 * Builder for entities on a {@link Silo Silo instance}.
 *
 * @author Andreas Holstenson
 *
 */
public interface EntityBuilder<Parent>
{
	/**
	 * Indicate that this entity should become a {@link BinaryEntity}.
	 *
	 * @return
	 */
	BinaryBuilder<Parent> asBinary();

	/**
	 * Indicate that this entity should become a {@link StructuredEntity}.
	 *
	 * @return
	 */
	StructuredEntityBuilder<Parent> asStructured();
}
