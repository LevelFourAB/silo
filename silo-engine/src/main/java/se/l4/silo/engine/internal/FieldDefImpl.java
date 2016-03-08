package se.l4.silo.engine.internal;

import se.l4.silo.engine.FieldDef;
import se.l4.silo.engine.types.FieldType;

public class FieldDefImpl
	implements FieldDef
{
	private final String name;
	private final FieldType<?> type;
	private final boolean collection;

	public FieldDefImpl(String name, FieldType<?> type, boolean collection)
	{
		this.name = name;
		this.type = type;
		this.collection = collection;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public FieldType<?> getType()
	{
		return type;
	}

	@Override
	public boolean isCollection()
	{
		return collection;
	}

}
