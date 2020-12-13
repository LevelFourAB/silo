package se.l4.silo.internal;

import se.l4.silo.EntityRef;
import se.l4.ylem.types.reflect.TypeRef;

public class EntityRefImpl<ID, T>
	implements EntityRef<ID, T>
{
	private final String name;
	private final TypeRef idType;
	private final TypeRef objectType;

	public EntityRefImpl(
		String name,
		TypeRef idType,
		TypeRef objectType
	)
	{
		this.name = name;
		this.idType = idType;
		this.objectType = objectType;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public TypeRef getIdType()
	{
		return idType;
	}

	@Override
	public TypeRef getDataType()
	{
		return objectType;
	}
}
