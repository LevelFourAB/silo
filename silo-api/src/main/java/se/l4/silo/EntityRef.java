package se.l4.silo;

import se.l4.silo.internal.EntityRefImpl;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

public interface EntityRef<ID, T>
{
	/**
	 * Get the name of the entity.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the type of identifier in use for this entity.
	 *
	 * @return
	 */
	TypeRef getIdType();

	/**
	 * Get the type of data the entity stores.
	 *
	 * @return
	 */
	TypeRef getDataType();

	/**
	 * Create a new reference to an entity.
	 *
	 * @param <ID>
	 * @param <T>
	 * @param name
	 * @param idType
	 * @param objectType
	 * @return
	 */
	static <ID, T> EntityRef<ID, T> create(String name, Class<ID> idType, Class<T> objectType)
	{
		return new EntityRefImpl<>(name, Types.reference(idType), Types.reference(objectType));
	}

	static <ID, T> EntityRef<ID, T> create(String name, TypeRef idType, TypeRef objectType)
	{
		return new EntityRefImpl<>(name, idType, objectType);
	}

	static <ID> EntityRef<ID, Blob<ID>> forBlob(String name, Class<ID> idType)
	{
		return new EntityRefImpl<>(name, Types.reference(idType), Types.reference(Blob.class, idType));
	}
}
