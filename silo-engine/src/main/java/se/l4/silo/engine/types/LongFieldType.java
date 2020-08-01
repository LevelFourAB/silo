package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.exobytes.Serializer;
import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataOutput;

public class LongFieldType
	implements FieldType<Long>
{
	public static final FieldType<Long> INSTANCE = new LongFieldType();

	@Override
	public String uniqueId()
	{
		return "long";
	}

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
	public Serializer<Long> getSerializer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int estimateMemory(Long instance)
	{
		return 30;
	}

	@Override
	public Long read(ExtendedDataInput in)
		throws IOException
	{
		return in.readLong();
	}

	@Override
	public void write(Long instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeLong(instance);
	}

}
