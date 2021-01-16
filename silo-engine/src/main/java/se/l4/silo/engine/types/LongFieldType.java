package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class LongFieldType
	implements FieldType<Long>
{
	public static final FieldType<Long> INSTANCE = new LongFieldType();

	@Override
	public int compare(Long o1, Long o2)
	{
		if(o1 == null && o2 == null) return 0;
		if(o1 == null) return -1;
		if(o2 == null) return 1;

		return Long.compare(o1, o2);
	}

	@Override
	public Long convert(Object in)
	{
		if(in instanceof Long) return (Long) in;
		if(in instanceof Number) return ((Number) in).longValue();

		throw new IllegalArgumentException("Can't convert " + in + " to a long");
	}

	@Override
	public int estimateMemory(Long instance)
	{
		return 30;
	}

	@Override
	public Long read(BinaryDataInput in)
		throws IOException
	{
		return in.readLong();
	}

	@Override
	public void write(Long instance, BinaryDataOutput out)
		throws IOException
	{
		out.writeLong(instance);
	}

	@Override
	public Long nextDown(Long in)
	{
		return Math.addExact(in, -1);
	}

	@Override
	public Long nextUp(Long in)
	{
		return Math.addExact(in, 1);
	}
}
