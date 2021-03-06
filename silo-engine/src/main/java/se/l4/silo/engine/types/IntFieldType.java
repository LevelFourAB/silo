package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class IntFieldType
	implements FieldType<Integer>
{
	public static final FieldType<Integer> INSTANCE = new IntFieldType();

	@Override
	public int compare(Integer o1, Integer o2)
	{
		if(o1 == null && o2 == null) return 0;
		if(o1 == null) return -1;
		if(o2 == null) return 1;

		return Integer.compare(o1, o2);
	}

	@Override
	public Integer convert(Object in)
	{
		if(in instanceof Integer) return (Integer) in;
		if(in instanceof Number)
		{
			long l = ((Number) in).longValue();
			if(l == Long.MIN_VALUE) return Integer.MIN_VALUE;
			if(l == Long.MAX_VALUE) return Integer.MAX_VALUE;
			return (int) l;
		}

		throw new IllegalArgumentException("Can't convert " + in + " to a int");
	}

	@Override
	public int estimateMemory(Integer instance)
	{
		return 24;
	}

	@Override
	public void write(Integer instance, BinaryDataOutput out)
		throws IOException
	{
		out.writeInt(instance);
	}

	@Override
	public Integer read(BinaryDataInput in)
		throws IOException
	{
		return in.readInt();
	}

	@Override
	public Integer nextDown(Integer in)
	{
		return Math.addExact(in, -1);
	}

	@Override
	public Integer nextUp(Integer in)
	{
		return Math.addExact(in, 1);
	}
}
