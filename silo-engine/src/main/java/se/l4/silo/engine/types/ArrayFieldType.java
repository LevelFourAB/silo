package se.l4.silo.engine.types;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.collections.api.factory.Lists;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class ArrayFieldType
	implements FieldType<Object[]>
{
	private static final byte NULL = 0;
	private static final byte VALUE = 1;

	@SuppressWarnings("rawtypes")
	private final FieldType type;

	@SuppressWarnings("rawtypes")
	public ArrayFieldType(FieldType type)
	{
		this.type = type;
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

			@SuppressWarnings("unchecked")
			int c = type.compare(a, b);
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object[] convert(Object in)
	{
		if(in instanceof Collection)
		{
			return subConvert(((Collection) in).toArray());
		}
		else if(in instanceof Iterable)
		{
			return subConvert(Lists.mutable.ofAll((Iterable) in).toArray());
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
	@SuppressWarnings("unchecked")
	public void write(Object[] instance, BinaryDataOutput out)
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
	public Object[] read(BinaryDataInput in)
		throws IOException
	{
		int n = in.readVInt();
		Object[] result = new Object[n];
		for(int i=0; i<n; i++)
		{
			int tag = in.read();
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

	@Override
	public Object[] nextDown(Object[] in)
	{
		throw new UnsupportedOperationException("arrays can not be compared");
	}

	@Override
	public Object[] nextUp(Object[] in)
	{
		throw new UnsupportedOperationException("arrays can not be compared");
	}
}
