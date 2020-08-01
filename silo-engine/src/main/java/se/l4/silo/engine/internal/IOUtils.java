package se.l4.silo.engine.internal;

import java.io.IOException;

import se.l4.silo.StorageException;
import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataOutput;

/**
 * Utilities to help with certain IO operations.
 *
 * @author Andreas Holstenson
 *
 */
public class IOUtils
{
	private IOUtils()
	{
	}

	public static void writeId(Object id, ExtendedDataOutput out)
		throws IOException
	{
		if(id instanceof Long)
		{
			out.write(1);
			out.writeLong((long) id);
		}
		else if(id instanceof Integer)
		{
			out.write(2);
			out.writeInt((int) id);
		}
		else if(id instanceof String)
		{
			out.write(3);
			out.writeString(id.toString());
		}
		else if(id instanceof byte[])
		{
			out.write(4);
			writeByteArray((byte[]) id, out);
		}
		else if(id == null)
		{
			out.write(0);
		}
		else
		{
			throw new StorageException("Unsupported identifier of type " + id.getClass().getName() + ", value is " + id);
		}
	}

	public static Object readId(ExtendedDataInput in)
		throws IOException
	{
		int tag = in.readByte();
		switch(tag)
		{
			case 0:
				return null;
			case 1:
				return in.readLong();
			case 2:
				return in.readInt();
			case 3:
				return in.readString();
			case 4:
				return readByteArray(in);
			default:
				throw new StorageException("Unknown identifier type, tagged as " + tag + " in storage");
		}
	}

	public static void writeByteArray(byte[] data, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(data.length);
		out.write(data);
	}

	public static void writeByteArray(byte[] data, int offset, int length, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(length);
		out.write(data, offset, length);
	}

	public static byte[] readByteArray(ExtendedDataInput in)
		throws IOException
	{
		int len = in.readVInt();
		byte[] data = new byte[len];
		in.readFully(data);
		return data;
	}
}
