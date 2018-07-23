package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.commons.io.ExtendedDataInput;
import se.l4.commons.io.ExtendedDataOutput;
import se.l4.commons.serialization.Serializer;

public class BooleanFieldType
	implements FieldType<Boolean>
{
	public static final FieldType<Boolean> INSTANCE = new BooleanFieldType();

	@Override
	public String uniqueId()
	{
		return "boolean";
	}

	@Override
	public int compare(Boolean o1, Boolean o2)
	{
		if(o1 == null && o2 == null) return 0;
		if(o1 == null) return -1;
		if(o2 == null) return 1;

		return Boolean.compare(o1, o2);
	}

	@Override
	public Boolean convert(Object in)
	{
		if(in instanceof Boolean) return (Boolean) in;

		throw new IllegalArgumentException("Can't convert " + in + " to a boolean");
	}

	@Override
	public Serializer<Boolean> getSerializer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int estimateMemory(Boolean instance)
	{
		return 21;
	}

	@Override
	public void write(Boolean instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeBoolean(instance);
	}

	@Override
	public Boolean read(ExtendedDataInput in)
		throws IOException
	{
		return in.readBoolean();
	}

}
