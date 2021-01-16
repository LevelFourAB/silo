package se.l4.silo.engine.io;

import java.io.EOFException;
import java.io.IOException;

import se.l4.silo.StorageException;

public abstract class AbstractBinaryDataInput
	implements BinaryDataInput
{
	private static final int TRUE = 1;

	@Override
	public boolean readBoolean()
		throws IOException
	{
		int b = read();
		return b == TRUE;
	}

	@Override
	public double readDouble()
		throws IOException
	{
		long value = ((long) read()) |
			((long) read()) << 8 |
			((long) read()) << 16 |
			((long) read()) << 24 |
			((long) read()) << 32 |
			((long) read()) << 40 |
			((long) read()) << 48 |
			((long) read()) << 56;

		return Double.longBitsToDouble(value);
	}

	@Override
	public float readFloat()
		throws IOException
	{
		int value = read() |
			read() << 8 |
			read() << 16 |
			read() << 24;

		return Float.intBitsToFloat(value);
	}

	@Override
	public int readVInt()
		throws IOException
	{
		int shift = 0;
		int result = 0;
		while(shift < 32)
		{
			int b = read();
			result |= (b & 0x7F) << shift;
			if((b & 0x80) == 0) return result;

			shift += 7;
		}

		throw new EOFException("Invalid integer");
	}

	@Override
	public int readInt()
		throws IOException
	{
		int type = read();
		if(type == BinaryDataConstants.TAG_ZERO) return 0;

		if(type == BinaryDataConstants.TAG_POSITIVE)
		{
			return readVInt();
		}
		else if(type == BinaryDataConstants.TAG_NEGATIVE)
		{
			return -readVInt();
		}
		else
		{
			throw new EOFException("Unknown type: " + type);
		}
	}

	@Override
	public long readVLong()
		throws IOException
	{
		int shift = 0;
		long result = 0;
		while(shift < 64)
		{
			int b = read();
			result |= (long) (b & 0x7F) << shift;
			if((b & 0x80) == 0) return result;

			shift += 7;
		}

		throw new EOFException("Invalid long");
	}

	@Override
	public long readLong()
		throws IOException
	{
		int type = read();
		if(type == 0) return 0l;

		if(type == 1)
		{
			return readVLong();
		}
		else if(type == 2)
		{
			return -readVLong();
		}
		else
		{
			throw new EOFException("Unknown type: " + type);
		}
	}

	@Override
	public String readString()
		throws IOException
	{
		int length = readVInt();
		char[] chars = new char[length];

		for(int i=0; i<length; i++)
		{
			int c = read();
			int t = c >> 4;
			if(t > -1 && t < 8)
			{
				chars[i] = (char) c;
			}
			else if(t == 12 || t == 13)
			{
				chars[i] = (char) ((c & 0x1f) << 6 | read() & 0x3f);
			}
			else if(t == 14)
			{
				chars[i] = (char) ((c & 0x0f) << 12
					| (read() & 0x3f) << 6
					| (read() & 0x3f) << 0);
			}
		}

		return new String(chars, 0, length);
	}

	@Override
	public byte[] readByteArray()
		throws IOException
	{
		int length = readVInt();
		byte[] buffer = new byte[length];

		int current = 0;
		while(current < length)
		{
			int l = read(buffer, current, length - current);
			if(l < 0)
			{
				throw new EOFException();
			}

			current += l;
		}

		return buffer;
	}

	@Override
	public Object readId()
		throws IOException
	{
		int tag = read();
		switch(tag)
		{
			case BinaryDataConstants.TAG_ID_NULL:
				return null;
			case BinaryDataConstants.TAG_ID_LONG:
				return readLong();
			case BinaryDataConstants.TAG_ID_INT:
				return readInt();
			case BinaryDataConstants.TAG_ID_STRING:
				return readString();
			case BinaryDataConstants.TAG_ID_BYTE_ARRAY:
				return readByteArray();
			default:
				throw new StorageException("Unknown identifier type, tagged as " + tag + " in storage");
		}
	}
}
