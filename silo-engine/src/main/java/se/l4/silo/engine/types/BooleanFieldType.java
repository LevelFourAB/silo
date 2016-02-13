package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.aurochs.core.io.ExtendedDataInput;
import se.l4.aurochs.core.io.ExtendedDataOutput;
import se.l4.aurochs.serialization.Serializer;

public class BooleanFieldType
	implements FieldType<Boolean>
{
	@Override
	public String uniqueId()
	{
		return "boolean";
	}

	@Override
	public int compare(Boolean o1, Boolean o2)
	{
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
