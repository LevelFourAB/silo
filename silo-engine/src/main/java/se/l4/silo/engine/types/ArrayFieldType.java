package se.l4.silo.engine.types;

import java.io.IOException;
import java.util.Collection;

import com.google.common.collect.Lists;

import se.l4.commons.io.ExtendedDataInput;
import se.l4.commons.io.ExtendedDataOutput;
import se.l4.commons.serialization.Serializer;

public class ArrayFieldType
	implements FieldType<Object[]>
{
	private static final byte NULL = 0;
	private static final byte VALUE = 1;
	
	private final FieldType type;

	@SuppressWarnings("rawtypes")
	public ArrayFieldType(FieldType type)
	{
		this.type = type;
	}
	
	@Override
	public String uniqueId()
	{
		return "merged:" + type.uniqueId();
	}

	@Override
	public int compare(Object[] o1, Object[] o2)
	{
		for(int i=0, n=Math.min(o1.length, o2.length); i<n; i++)
		{
			Object a = o1[i];
			Object b = o2[i];
			
			if(a == b) continue;

			if(a == MaxMin.MIN)
			{
				return -1;
			}
			else if(a == MaxMin.MAX)
			{
				return 1;
			}
			else if(b == MaxMin.MIN)
			{
				return 1;
			}
			else if(b == MaxMin.MAX)
			{
				return -1;
			}
			
			int c = type.compare(a, b);
			if(c != 0)
			{
				return c;
			}
		}
		
		return 0;
	}

	@Override
	public int estimateMemory(Object[] instance)
	{
		int size = 64;
		for(Object o : instance)
		{
			if(o != null)
			{
				size += type.estimateMemory(o);
			}
		}
		
		return size;
	}
	
	private Object[] subConvert(Object[] in)
	{
		for(int i=0, n=in.length; i<n; i++)
		{
			in[i] = type.convert(in[i]);
		}
		return in;
	}

	@Override
	public Object[] convert(Object in)
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
			return new Object[] { in == null ? null : type.convert(in) };
		}
	}

	@Override
	public Serializer<Object[]> getSerializer()
	{
		// TODO: Create a custom serializer?
		return null;
	}

	@Override
	public void write(Object[] instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(instance.length);
		for(int i=0, n=instance.length; i<n; i++)
		{
			if(instance[i] == null)
			{
				out.write(NULL);
			}
			else
			{
				out.write(VALUE);
				type.write(instance[i], out);
			}
		}
	}

	@Override
	public Object[] read(ExtendedDataInput in)
		throws IOException
	{
		int n = in.readVInt();
		Object[] result = new Object[n];
		for(int i=0; i<n; i++)
		{
			int tag = in.readByte();
			if(tag == NULL)
			{
				result[i] = null;
			}
			else
			{
				result[i] = type.read(in);
			}
		}
		return result;
	}

}
