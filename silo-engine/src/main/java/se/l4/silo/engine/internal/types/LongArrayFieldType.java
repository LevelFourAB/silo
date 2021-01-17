package se.l4.silo.engine.internal.types;

import java.nio.ByteBuffer;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.StorageException;

/**
 * {@link DataType} used for encoding arrays of longs.
 */
public class LongArrayFieldType
	implements DataType
{
	public static final LongArrayFieldType INSTANCE = new LongArrayFieldType();

	private static final byte TAG_LEGACY_0_2 = 0x01;
	private static final byte TAG_CURRENT = 0x02;

	private LongArrayFieldType()
	{
	}

	@Override
	public int compare(Object o1, Object o2)
	{
		long[] l1 = (long[]) o1;
		long[] l2 = (long[]) o2;

		for(int i=0, n=Math.min(l1.length, l2.length); i<n; i++)
		{
			long a = l1[i];
			long b = l2[i];

			int c = Long.compare(a, b);
			if(c != 0)
			{
				return c;
			}
		}

		return 0;
	}

	@Override
	public int getMemory(Object instance)
	{
		return 64 + ((long[]) instance).length * 8;
	}

	@Override
	public void write(WriteBuffer buff, Object obj)
	{
		buff.put(TAG_CURRENT);

		long[] values = (long[]) obj;
		buff.putVarInt(values.length);

		for(long l : values)
		{
			buff.putVarLong(l);
		}
	}

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			write(buff, obj[i]);
		}
	}

	@Override
	public long[] read(ByteBuffer in)
	{
		// Read the byte representing if this is value or not
		byte tag = in.get();
		if(tag == TAG_LEGACY_0_2)
		{
			// Legacy: Version number - ignore
			DataUtils.readVarInt(in);

			// Actual array
			int n = DataUtils.readVarInt(in);
			long[] result = new long[n];

			for(int i=0; i<n; i++)
			{
				result[i] = KeyLongType.readLegacyLong(in);
			}

			return result;
		}
		else if(tag == TAG_CURRENT)
		{
			int n = DataUtils.readVarInt(in);
			long[] result = new long[n];

			for(int i=0; i<n; i++)
			{
				result[i] = DataUtils.readVarLong(in);
			}

			return result;
		}

		throw new StorageException();
	}

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			obj[i] = read(buff);
		}
	}
}
