package se.l4.silo.engine.internal.types;

import java.nio.ByteBuffer;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

/**
 * {@link DataType} that stores a non-null positive long.
 */
public class PositiveLongType
	implements DataType
{
	public static final DataType INSTANCE = new PositiveLongType();

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
		return DataUtils.readVarLong(buff);
	}

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			obj[i] = DataUtils.readVarLong(buff);
		}
	}

	@Override
	public void write(WriteBuffer buff, Object obj)
	{
		buff.putVarLong((Long) obj);
	}

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			buff.putVarLong((Long) obj[i]);
		}
	}
}
