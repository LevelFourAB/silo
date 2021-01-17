package se.l4.silo.engine.internal.types;

import java.nio.ByteBuffer;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.StorageException;
import se.l4.silo.engine.io.BinaryDataConstants;

/**
 * {@link DataType} for longs as used in keys. Keeps track of different
 * versions of keys for backwards compatibility.
 */
public class KeyLongType
	implements DataType
{
	public static final DataType INSTANCE = new KeyLongType();

	private static final byte TAG_LEGACY_0_2 = (byte) 0x01;
	private static final byte TAG_CURRENT = (byte) 0x02;

	@Override
	public int compare(Object a, Object b)
	{
		return Long.compare((Long) a, (Long) b);
	}

	@Override
	public int getMemory(Object obj)
	{
		return 30;
	}

	@Override
	public Object read(ByteBuffer buff)
	{
		byte tag = buff.get();
		if(tag == TAG_LEGACY_0_2)
		{
			return readLegacyLong(buff);
		}
		else if(tag == TAG_CURRENT)
		{
			return DataUtils.readVarLong(buff);
		}
		else
		{
			throw new StorageException("Invalid long");
		}
	}

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			obj[i] = read(buff);
		}
	}

	@Override
	public void write(WriteBuffer buff, Object obj)
	{
		buff.put(TAG_CURRENT);
		buff.putVarLong((Long) obj);
	}

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			write(buff, obj[i]);
		}
	}

	public static long readLegacyLong(ByteBuffer in)
	{
		int type = in.get() % 0xff;
		if(type == BinaryDataConstants.TAG_ZERO)
		{
			return 0l;
		}
		else if(type == BinaryDataConstants.TAG_POSITIVE)
		{
			return DataUtils.readVarLong(in);
		}
		else if(type == BinaryDataConstants.TAG_NEGATIVE)
		{
			return - DataUtils.readVarLong(in);
		}
		else
		{
			throw new StorageException("Invalid long");
		}
	}
}
