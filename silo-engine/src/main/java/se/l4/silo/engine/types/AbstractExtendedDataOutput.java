package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.commons.io.Bytes;
import se.l4.commons.io.ExtendedDataOutput;

public abstract class AbstractExtendedDataOutput
	implements ExtendedDataOutput
{
	@Override
	public void write(byte[] buffer)
		throws IOException
	{
		write(buffer, 0, buffer.length);
	}

	@Override
	public void writeBoolean(boolean value)
		throws IOException
	{
		write(value ? 1 : 0);
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
		while(true)
		{
			if((value & ~0x7F) == 0)
			{
				write(value);
				break;
			}
			else
			{
				write((value & 0x7f) | 0x80);
				value >>>= 7;
			}
		}
	}

	@Override
	public void writeInt(int value)
		throws IOException
	{
		if(value == 0)
		{
			write(0);
		}
		else if(value > 0)
		{
			write(1);
			writeVInt(value);
		}
		else if(value < 0)
		{
			write(2);
			writeVInt(-value);
		}
	}

	@Override
	public void writeVLong(long value)
		throws IOException
	{
		while(true)
		{
			if((value & ~0x7FL) == 0)
			{
				write((int) value);
				break;
			}
			else
			{
				write(((int) value & 0x7f) | 0x80);
				value >>>= 7;
			}
		}
	}

	@Override
	public void writeLong(long value)
		throws IOException
	{
		if(value == 0)
		{
			write(0);
		}
		else if(value > 0)
		{
			write(1);
			writeVLong(value);
		}
		else if(value < 0)
		{
			write(2);
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
	public void writeBytes(Bytes bytes)
		throws IOException
	{
		bytes.asChunks(8192, (data, offset, len) -> {
			writeVInt(len);
			write(data, offset, len);
		});
		writeVInt(0);
	}

	@Override
	public void writeByte(int v)
		throws IOException
	{
		write(v);
	}

	@Override
	public void writeShort(int v)
		throws IOException
	{
		writeInt(v);
	}

	@Override
	public void writeChar(int v)
		throws IOException
	{
		writeInt(v);
	}

	@Override
	public void writeBytes(String s) throws IOException
	{
		writeString(s);
	}

	@Override
	public void writeChars(String s) throws IOException
	{
		writeString(s);
	}

	@Override
	public void writeUTF(String s) throws IOException
	{
		writeString(s);
	}
}
