package se.l4.silo.engine.io;

import java.io.IOException;

import se.l4.silo.StorageException;

public abstract class AbstractBinaryDataOutput
	implements BinaryDataOutput
{
	@Override
	public void writeBoolean(boolean value)
		throws IOException
	{
		write(value ? BinaryDataConstants.TAG_BOOL_TRUE : BinaryDataConstants.TAG_BOOL_FALSE);
	}

	@Override
	public void writeDouble(double value)
		throws IOException
	{
		long l = Double.doubleToRawLongBits(value);
		write((int) l & 0xff);
		write((int) (l >> 8) & 0xff);
		write((int) (l >> 16) & 0xff);
		write((int) (l >> 24) & 0xff);
		write((int) (l >> 32) & 0xff);
		write((int) (l >> 40) & 0xff);
		write((int) (l >> 48) & 0xff);
		write((int) (l >> 56) & 0xff);
	}

	@Override
	public void writeFloat(float value)
		throws IOException
	{
		int i = Float.floatToRawIntBits(value);
		write(i & 0xff);
		write((i >> 8) & 0xff);
		write((i >> 16) & 0xff);
		write((i >> 24) & 0xff);
	}

	@Override
	public void writeVInt(int value)
		throws IOException
	{
		while((value & ~0x7f) != 0)
		{
			write((byte) (value | 0x80));
			value >>>= 7;
		}

		write(value);
	}

	@Override
	public void writeInt(int value)
		throws IOException
	{
		if(value == 0)
		{
			write(BinaryDataConstants.TAG_ZERO);
		}
		else if(value > 0)
		{
			write(BinaryDataConstants.TAG_POSITIVE);
			writeVInt(value);
		}
		else
		{
			write(BinaryDataConstants.TAG_NEGATIVE);
			writeVInt(-value);
		}
	}

	@Override
	public void writeVLong(long value)
		throws IOException
	{
		while((value & ~0x7f) != 0)
		{
			write((byte) (value | 0x80));
			value >>>= 7;
		}

		write((byte) value);
	}

	@Override
	public void writeLong(long value)
		throws IOException
	{
		if(value == 0)
		{
			write(BinaryDataConstants.TAG_ZERO);
		}
		else if(value > 0)
		{
			write(BinaryDataConstants.TAG_POSITIVE);
			writeVLong(value);
		}
		else
		{
			write(BinaryDataConstants.TAG_NEGATIVE);
			writeVLong(-value);
		}
	}

	@Override
	public void writeString(String value)
		throws IOException
	{
		writeVInt(value.length());
		for(int i=0, n=value.length(); i<n; i++)
		{
			char c = value.charAt(i);
			if(c <= 0x007f)
			{
				write((byte) c);
			}
			else if(c > 0x07ff)
			{
				write((byte) (0xe0 | c >> 12 & 0x0f));
				write((byte) (0x80 | c >> 6 & 0x3f));
				write((byte) (0x80 | c >> 0 & 0x3f));
			}
			else
			{
				write((byte) (0xc0 | c >> 6 & 0x1f));
				write((byte) (0x80 | c >> 0 & 0x3f));
			}
		}
	}

	@Override
	public void writeId(Object object)
		throws IOException
	{
		if(object instanceof Long)
		{
			write(BinaryDataConstants.TAG_ID_LONG);
			writeLong((long) object);
		}
		else if(object instanceof Integer)
		{
			write(BinaryDataConstants.TAG_ID_INT);
			writeInt((int) object);
		}
		else if(object instanceof String)
		{
			write(BinaryDataConstants.TAG_ID_STRING);
			writeString(object.toString());
		}
		else if(object instanceof byte[])
		{
			write(BinaryDataConstants.TAG_ID_BYTE_ARRAY);
			writeByteArray((byte[]) object);
		}
		else if(object == null)
		{
			write(BinaryDataConstants.TAG_ID_NULL);
		}
		else
		{
			throw new StorageException("Unsupported identifier of type " + object.getClass().getName() + ", value is " + object);
		}
	}

	@Override
	public void writeByteArray(byte[] data)
		throws IOException
	{
		writeByteArray(data, 0, data.length);
	}

	@Override
	public void writeByteArray(byte[] data, int offset, int length)
		throws IOException
	{
		writeVInt(length);
		write(data, offset, length);
	}
}
