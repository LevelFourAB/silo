package se.l4.silo;

import se.l4.silo.internal.CollectionRefImpl;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

/**
 * Reference to a collection.
 */
public interface CollectionRef<ID, T>
{
	/**
	 * Get the name of the collection.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the type of identifier in use for this collection.
	 *
	 * @return
	 */
	TypeRef getIdType();

	/**
	 * Get the type of data the collection stores.
	 *
	 * @return
	 */
	TypeRef getDataType();

	/**
	 * Create a new reference to a collection.
	 *
	 * @param <ID>
	 * @param <T>
	 * @param name
	 * @param idType
	 * @param objectType
	 * @return
	 */
	static <ID, T> CollectionRef<ID, T> create(String name, Class<ID> idType, Class<T> objectType)
	{
		return new CollectionRefImpl<>(name, Types.reference(idType), Types.reference(objectType));
	}

	/**
	 * Create a new reference to a collection.
	 *
	 * @param <ID>
	 * @param <T>
	 * @param name
	 * @param idType
	 * @param objectType
	 * @return
	 */
	static <ID, T> CollectionRef<ID, T> create(String name, TypeRef idType, TypeRef objectType)
	{
		return new CollectionRefImpl<>(name, idType, objectType);
	}
}
