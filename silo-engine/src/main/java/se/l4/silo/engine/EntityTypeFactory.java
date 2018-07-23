package se.l4.silo.engine;

import se.l4.silo.Entity;

/**
 * Factory for creating instances of {@link Entity}.
 *
 * @author Andreas Holstenson
 *
 * @param <T>
 *   the type of the entity created
 * @param <Config>
 *   the type of the configuration for the entity, if there is no special
 *   configuration use {@link Void}.
 */
public interface EntityTypeFactory<T extends Entity, Config>
{
	/**
	 * Get the identifier of this type.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Get the type of configuration needed for this entity.
	 *
	 * @return
	 */
	Class<Config> getConfigType();

	/**
	 * Create an instance of this entity over the given {@link Storage}.
	 *
	 * @param entity
	 * @return
	 */
	T create(EntityCreationEncounter<Config> encounter);
}
