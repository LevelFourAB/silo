package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class MergedFieldType
	implements FieldType<Object[]>
{
	private static final byte NULL = 0;
	private static final byte VALUE = 1;

	@SuppressWarnings("rawtypes")
	private final FieldType[] types;

	@SuppressWarnings("rawtypes")
	public MergedFieldType(FieldType... types)
	{
		this.types = types;
	}

	@SuppressWarnings("rawtypes")
	public FieldType[] getTypes()
	{
		return types;
	}

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public void write(Object[] instance, BinaryDataOutput out)
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
	public Object[] read(BinaryDataInput in)
		throws IOException
	{
		Object[] result = new Object[types.length];
		int n = in.read();
		for(int i=0; i<n; i++)
		{
			int tag = in.read();
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

	@Override
	public Object[] nextDown(Object[] in)
	{
		throw new UnsupportedOperationException("merged types can not be compared");
	}

	@Override
	public Object[] nextUp(Object[] in)
	{
		throw new UnsupportedOperationException("merged types can not be compared");
	}
}
