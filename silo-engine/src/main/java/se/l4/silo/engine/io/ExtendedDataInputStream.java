package se.l4.silo.engine.io;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import se.l4.ylem.io.Bytes;

/**
 * {@link ExtendedDataInput} implemented on top of a {@link InputStream}.
 */
public class ExtendedDataInputStream
	extends DataInputStream
	implements ExtendedDataInput
{
	private static final int CHARS_SIZE = 1024;
	private static final ThreadLocal<char[]> CHARS = new ThreadLocal<char[]>()
	{
		@Override
		protected char[] initialValue()
		{
			return new char[1024];
		}
	};

	/**
	 * Create a new instance over the given input stream.
	 */
	public ExtendedDataInputStream(InputStream in)
	{
		super(in);
	}

	@Override
	public int read()
		throws IOException
	{
		return super.read();
	}

	@Override
	public int readVInt()
		throws IOException
	{
		int shift = 0;
		int result = 0;
		while(shift < 32)
		{
			final byte b = (byte) read();
			result |= (b & 0x7F) << shift;
			if((b & 0x80) == 0) return result;

			shift += 7;
		}

		throw new EOFException("Invalid integer");
	}

	@Override
	public long readVLong()
		throws IOException
	{
		int shift = 0;
		long result = 0;
		while(shift < 64)
		{
			final byte b = (byte) read();
			result |= (long) (b & 0x7F) << shift;
			if((b & 0x80) == 0) return result;

			shift += 7;
		}

		throw new EOFException("Invalid long");
	}

	@Override
	public String readString()
		throws IOException
	{
		int length = readVInt();
		char[] chars = length < CHARS_SIZE ? CHARS.get() : new char[length];

		for(int i=0; i<length; i++)
		{
			int c = in.read() & 0xff;
			int t = c >> 4;
			if(t > -1 && t < 8)
			{
				chars[i] = (char) c;
			}
			else if(t == 12 || t == 13)
			{
				chars[i] = (char) ((c & 0x1f) << 6 | in.read() & 0x3f);
			}
			else if(t == 14)
			{
				chars[i] = (char) ((c & 0x0f) << 12
					| (in.read() & 0x3f) << 6
					| (in.read() & 0x3f) << 0);
			}
		}

		return new String(chars, 0, length);
	}

	@Override
	public Bytes readBytes()
		throws IOException
	{
		return Bytes.capture(out -> {
			byte[] buffer = new byte[8192];
			while(true)
			{
				int len = readVInt();
				if(len == 0) return;

				readFully(buffer, 0, len);
				out.write(buffer, 0, len);
			}
		});
	}

	@Override
	public Bytes readTemporaryBytes()
		throws IOException
	{
		return new TemporaryBytes(this);
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
