package se.l4.silo.engine;

import se.l4.commons.serialization.SerializerCollection;
import se.l4.silo.engine.builder.StorageBuilder;

/**
 * Encounter for top level entity types to help with creating and managing
 * underlying storage.
 *
 * @author Andreas Holstenson
 *
 */
public interface EntityCreationEncounter<Config>
{
	/**
	 * Get the name of the entity being built.
	 *
	 * @return
	 */
	String getEntityName();

	/**
	 * Get the configuration for this entity.
	 *
	 * @return
	 */
	Config getConfig();

	/**
	 * Get the {@link SerializerCollection} in use for this entity.
	 *
	 * @return
	 */
	SerializerCollection getSerializerCollection();

	/**
	 * Create the main storage for this entity.
	 *
	 * @return
	 */
	StorageBuilder createMainEntity();

	/**
	 * Create a storage that can be used
	 * @param sub
	 * @return
	 */
	StorageBuilder createSubEntity(String sub);

}
