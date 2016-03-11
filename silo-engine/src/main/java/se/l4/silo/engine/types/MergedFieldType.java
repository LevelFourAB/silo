package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.aurochs.core.io.ExtendedDataInput;
import se.l4.aurochs.core.io.ExtendedDataOutput;
import se.l4.aurochs.serialization.Serializer;

public class MergedFieldType
	implements FieldType<Object[]>
{
	private static final byte NULL = 0;
	private static final byte VALUE = 1;
	
	private final FieldType[] types;

	@SuppressWarnings("rawtypes")
	public MergedFieldType(FieldType... types)
	{
		this.types = types;
	}
	
	public FieldType[] getTypes()
	{
		return types;
	}
	
	@Override
	public String uniqueId()
	{
		return "merged";
	}

	@Override
	public int compare(Object[] o1, Object[] o2)
	{
		for(int i=0, n=types.length; i<n; i++)
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
			
			int c = types[i].compare(a, b);
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
		for(int i=0, n=types.length; i<n; i++)
		{
			Object o = instance[i];
			if(o != null)
			{
				size += types[i].estimateMemory(o);
			}
		}
		return size;
	}

	@Override
	public Object[] convert(Object in)
	{
		Object[] result = (Object[]) in;
		for(int i=0, n=result.length; i<n; i++)
		{
			result[i] = types[i].convert(result[i]);
		}
		return result;
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
		out.write(types.length);
		for(int i=0, n=types.length; i<n; i++)
		{
			if(instance[i] == null)
			{
				out.write(NULL);
			}
			else
			{
				out.write(VALUE);
				types[i].write(instance[i], out);
			}
		}
	}

	@Override
	public Object[] read(ExtendedDataInput in)
		throws IOException
	{
		Object[] result = new Object[types.length];
		int n = in.readUnsignedByte();
		for(int i=0; i<n; i++)
		{
			int tag = in.readUnsignedByte();
			if(tag == NULL)
			{
				result[i] = null;
			}
			else
			{
				result[i] = types[i].read(in);
			}
		}
		return result;
	}

}
