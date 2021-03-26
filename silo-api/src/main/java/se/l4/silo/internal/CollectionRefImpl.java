package se.l4.silo.internal;

import se.l4.silo.CollectionRef;
import se.l4.ylem.types.reflect.TypeRef;

public class CollectionRefImpl<ID, T>
	implements CollectionRef<ID, T>
{
	private final String name;
	private final TypeRef idType;
	private final TypeRef objectType;

	public CollectionRefImpl(
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
