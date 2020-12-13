package se.l4.silo.engine;

/**
 * Encounter for top level entity types to help with creating and managing
 * underlying storage.
 */
public interface EntityCreationEncounter
{
	/**
	 * Get the name of the entity being built.
	 *
	 * @return
	 */
	String getEntityName();

	/**
	 * Create the main storage for this entity.
	 *
	 * @return
	 */
	<T> Storage.Builder<T> createMainEntity(EntityCodec<T> codec);

	/**
	 * Create a storage that can be used
	 * @param sub
	 * @return
	 */
	<T> Storage.Builder<T> createSubEntity(String sub, EntityCodec<T> codec);
}
