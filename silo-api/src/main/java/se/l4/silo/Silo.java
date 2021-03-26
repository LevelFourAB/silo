package se.l4.silo;

/**
 * Silo storage, provides access to all collections and transactions. An
 * instance of this class contains {@link Collection collections} that can be
 * used to store and retrieve data.
 */
public interface Silo
{
	/**
	 * Check if the given collection is available.
	 *
	 * @param name
	 * @return
	 */
	boolean hasCollection(String name);

	/**
	 * Get a collection.
	 *
	 * @param ref
	 *   reference to the collection
	 * @return
	 *   found collection
	 * @throws StorageException
	 *   if collection can not be found
	 */
	<ID, T> Collection<ID, T> getCollection(CollectionRef<ID, T> ref);

	/**
	 * Get a collection.
	 *
	 * @param name
	 *   name
	 * @param idType
	 *   the type of ids used
	 * @param objectType
	 *   the type of object stored
	 * @return
	 *   found collection
	 * @throws StorageException
	 *   if collection can not be found
	 */
	default <ID, T> Collection<ID, T> getCollection(String name, Class<ID> idType, Class<T> objectType)
	{
		return getCollection(CollectionRef.create(name, idType, objectType));
	}

	/**
	 * Access support for transactions.
	 *
	 * @return
	 */
	Transactions transactions();
}
