package se.l4.silo.engine.builder;

import se.l4.silo.Silo;

/**
 * Builder for entities on a {@link Silo Silo instance}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EntityBuilder
{
	BinaryBuilder asBinary();
}
