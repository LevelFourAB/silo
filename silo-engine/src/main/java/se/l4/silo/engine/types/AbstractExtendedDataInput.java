package se.l4.silo.engine.types;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import se.l4.commons.io.Bytes;
import se.l4.commons.io.BytesBuilder;
import se.l4.commons.io.ExtendedDataInput;

public abstract class AbstractExtendedDataInput
	implements ExtendedDataInput
{
	private static final int FALSE = 0;
	private static final int TRUE = 1;
	
	@Override
	public void readFully(byte[] buffer)
		throws IOException
	{
		readFully(buffer, 0, buffer.length);
	}
	
	@Override
	public double readDouble()
		throws IOException
	{
		long value = ((long) readByte() & 0xff) |
			((long) readByte() & 0xff) << 8 |
			((long) readByte() & 0xff) << 16 |
			((long) readByte() & 0xff) << 24 |
			((long) readByte() & 0xff) << 32 |
			((long) readByte() & 0xff) << 40 |
			((long) readByte() & 0xff) << 48 |
			((long) readByte() & 0xff) << 56;
		
		return Double.longBitsToDouble(value);
	}
	
	@Override
	public float readFloat()
		throws IOException
	{
		int value = (readByte() & 0xff) |
			(readByte() & 0xff) << 8 |
			(readByte() & 0xff) << 16 |
			(readByte() & 0xff) << 24;
		
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
			final byte b = readByte();
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
		int type = readByte();
		if(type == 0) return 0;
		
		if(type == 1)
		{
			return readVInt();
		}
		else if(type == 2)
		{
			return -readVInt();
		}
		else
		{
			throw new EOFException("Unknownn type: " + type);
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
			final byte b = readByte();
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
		int type = readByte();
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
			int c = readByte() & 0xff;
			int t = c >> 4;
			if(t > -1 && t < 8)
			{
				chars[i] = (char) c;
			}
			else if(t == 12 || t == 13)
			{
				chars[i] = (char) ((c & 0x1f) << 6 | readByte() & 0x3f);
			}
			else if(t == 14)
			{
				chars[i] = (char) ((c & 0x0f) << 12 
					| (readByte() & 0x3f) << 6
					| (readByte() & 0x3f) << 0);
			}
		}
		
		return new String(chars, 0, length);
	}
	
	@Override
	public boolean readBoolean()
		throws IOException
	{
		return readByte() == TRUE;
	}
	
	@Override
	public Bytes readBytes()
		throws IOException
	{
		BytesBuilder builder = Bytes.create();
		byte[] buffer = new byte[8192];
		while(true)
		{
			int len = readVInt();
			if(len == 0) return builder.build();
			
			readFully(buffer, 0, len);
			builder.addChunk(buffer, 0, len);
		}
	}
	
	@Override
	public Bytes readTemporaryBytes()
		throws IOException
	{
		return new TemporaryBytes(this);
	}

	@Override
	public int readUnsignedByte() throws IOException
	{
		return readByte();
	}


	@Override
	public short readShort()
		throws IOException
	{
		return (short) readInt();
	}


	@Override
	public int readUnsignedShort() throws IOException
	{
		short s = readShort();
		return s;
	}


	@Override
	public char readChar() throws IOException
	{
		return (char) readInt();
	}


	@Override
	public String readLine() throws IOException
	{
		return readString();
	}


	@Override
	public String readUTF() throws IOException
	{
		return readString();
	}

	
	private static class TemporaryBytes
		implements Bytes
	{
		private final ExtendedDataInput parent;

		public TemporaryBytes(ExtendedDataInput parent)
		{
			this.parent = parent;
		}
		
		@Override
		public InputStream asInputStream()
			throws IOException
		{
			return new TemporaryInputStream(parent);
		}
		
		@Override
		public byte[] toByteArray()
			throws IOException
		{
			throw new UnsupportedOperationException();
		}
	}
	
	private static class TemporaryInputStream
		extends InputStream
	{
		private final ExtendedDataInput in;
		private int remaining;

		public TemporaryInputStream(ExtendedDataInput in)
			throws IOException
		{
			this.in = in;
			this.remaining = 0;
		}
		
		@Override
		public int read()
			throws IOException
		{
			if(remaining == -1)
			{
				return -1;
			}
			else if(remaining == 0)
			{
				remaining = in.readVInt();
				if(remaining == 0)
				{
					remaining = -1;
					return -1;
				}
			}
			
			remaining--;
			return in.readUnsignedByte();
		}
		
		@Override
		public int read(byte[] b, int off, int len)
			throws IOException
		{
			if(remaining == -1)
			{
				return -1;
			}
			else if(remaining == 0)
			{
				remaining = in.readVInt();
				if(remaining == 0)
				{
					remaining = -1;
					return -1;
				}
			}
			
			len = Math.min(len, remaining);
			in.readFully(b, off, len);
			return len;
		}
	}
}
