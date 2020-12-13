package se.l4.silo.engine;

public interface DataEncounter<T>
{
	/**
	 * Get the object being indexed.
	 *
	 * @return
	 */
	T getObject();

	/**
	 * <strong>Expert</strong>: Get the main {@link Storage} instance for an entity.
	 *
	 * @param entity
	 * @return
	 */
	<S> Storage<S> getStorage(String entity);

	/**
	 * <strong>Expert</strong>: Get a {@link Storage} instance for an entity.
	 *
	 * @param entity
	 * @param name
	 * @return
	 */
	<S> Storage<S> getStorage(String entity, String name);
}
