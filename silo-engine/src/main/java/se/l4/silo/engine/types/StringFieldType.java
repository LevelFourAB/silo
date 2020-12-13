package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataOutput;

public class StringFieldType
	implements FieldType<String>
{
	public static final FieldType<String> INSTANCE = new StringFieldType();

	@Override
	public int compare(String o1, String o2)
	{
		if(o1 == null && o2 == null) return 0;
		if(o1 == null) return -1;
		if(o2 == null) return 1;

		return o1.compareTo(o2);
	}

	@Override
	public String convert(Object in)
	{
		return in == null ? null : in.toString();
	}

	@Override
	public int estimateMemory(String instance)
	{
		return 24 + 2 * instance.length();
	}

	@Override
	public String read(ExtendedDataInput in)
		throws IOException
	{
		return in.readString();
	}

	@Override
	public void write(String instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeString(instance);
	}

	@Override
	public String nextDown(String in)
	{
		throw new UnsupportedOperationException("strings can not be compared");
	}

	@Override
	public String nextUp(String in)
	{
		throw new UnsupportedOperationException("strings can not be compared");
	}
}
