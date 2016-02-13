package se.l4.silo.engine.builder;

import se.l4.silo.binary.BinaryEntity;

/**
 * Builder for creating a {@link BinaryEntity}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface BinaryBuilder
{
	/**
	 * Build and register the entity.
	 * 
	 * @return
	 */
	BinaryEntity build();
}
