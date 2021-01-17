package se.l4.silo.engine.internal.types;

import java.nio.ByteBuffer;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.StorageException;

/**
 * {@link DataType} used for {@code byte[]} chunks.
 */
public class ByteChunkFieldType
	implements DataType
{
	public static final DataType INSTANCE = new ByteChunkFieldType();

	private static final byte TAG_CURRENT = (byte) 0x01;

	@Override
	public int compare(Object a, Object b)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMemory(Object obj)
	{
		return 64 + ((byte[]) obj).length * 4;
	}

	@Override
	public void write(WriteBuffer buff, Object obj)
	{
		byte[] data = (byte[]) obj;
		buff.put(TAG_CURRENT);
		buff.putVarInt(data.length);
		buff.put(data);
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
	public Object read(ByteBuffer buff)
	{
		byte tag = buff.get();
		if(tag == TAG_CURRENT)
		{
			int length = DataUtils.readVarInt(buff);
			byte[] result = new byte[length];
			buff.get(result);
			return result;
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
}
