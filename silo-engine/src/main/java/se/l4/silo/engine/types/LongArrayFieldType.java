package se.l4.silo.engine.types;

import java.io.IOException;
import java.util.Collection;

import com.google.common.collect.Lists;

import se.l4.commons.io.ExtendedDataInput;
import se.l4.commons.io.ExtendedDataOutput;
import se.l4.commons.serialization.Serializer;

public class LongArrayFieldType
	implements FieldType<long[]>
{
	public LongArrayFieldType()
	{
	}
	
	@Override
	public String uniqueId()
	{
		return "long-array";
	}

	@Override
	public int compare(long[] o1, long[] o2)
	{
		for(int i=0, n=Math.min(o1.length, o2.length); i<n; i++)
		{
			long a = o1[i];
			long b = o2[i];
			
			int c = Long.compare(a, b);
			if(c != 0)
			{
				return c;
			}
		}
		
		return 0;
	}

	@Override
	public int estimateMemory(long[] instance)
	{
		int size = 64;
		for(Object o : instance)
		{
			if(o != null)
			{
				size += 8;
			}
		}
		
		return size;
	}
	
	private long[] subConvert(Object[] in)
	{
		long[] result = new long[in.length];
		for(int i=0, n=in.length; i<n; i++)
		{
			result[i] = LongFieldType.INSTANCE.convert(in[i]);
		}
		return result;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public long[] convert(Object in)
	{
		if(in instanceof Collection)
		{
			return subConvert(((Collection) in).toArray());
		}
		else if(in instanceof Iterable)
		{
			return subConvert(Lists.newArrayList((Iterable) in).toArray());
		}
		else if(in instanceof Object[])
		{
			return subConvert((Object[]) in);
		}
		else
		{
			return new long[] { LongFieldType.INSTANCE.convert(in) };
		}
	}

	@Override
	public Serializer<long[]> getSerializer()
	{
		// TODO: Create a custom serializer?
		return null;
	}

	@Override
	public void write(long[] instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(instance.length);
		for(int i=0, n=instance.length; i<n; i++)
		{
			out.writeLong(instance[i]);
		}
	}

	@Override
	public long[] read(ExtendedDataInput in)
		throws IOException
	{
		int n = in.readVInt();
		long[] result = new long[n];
		for(int i=0; i<n; i++)
		{
			result[i] = in.readLong();
		}
		return result;
	}

}
