package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class ByteArrayFieldType
	implements FieldType<byte[]>
{
	public static final FieldType<byte[]> INSTANCE = new ByteArrayFieldType();

	@Override
	public int compare(byte[] o1, byte[] o2)
	{
		if(o1 == null)
		{
			if(o2 == null)
			{
				return 0;
			}
			else
			{
				return -1;
			}
		}

		if(o2 == null)
		{
			return 1;
		}

		for(int i=0, n=Math.min(o1.length, o2.length); i<n; i++)
		{
			if(o1[i] < o2[i])
			{
				return -1;
			}
			else if(o1[i] > o2[i])
			{
				return 1;
			}
		}

		if(o1.length < o2.length)
		{
			return -1;
		}
		else if(o1.length > o2.length)
		{
			return 1;
		}

		return 0;
	}

	@Override
	public int estimateMemory(byte[] instance)
	{
		return 64 + instance.length * 4;
	}

	@Override
	public byte[] convert(Object in)
	{
		return (byte[]) in;
	}

	@Override
	public void write(byte[] instance, BinaryDataOutput out)
		throws IOException
	{
		out.writeByteArray(instance);
	}

	@Override
	public byte[] read(BinaryDataInput in)
		throws IOException
	{
		return in.readByteArray();
	}

	@Override
	public byte[] nextDown(byte[] in)
	{
		throw new UnsupportedOperationException("byte array can not be compared");
	}

	@Override
	public byte[] nextUp(byte[] in)
	{
		throw new UnsupportedOperationException("byt array can not be compared");
	}
}
