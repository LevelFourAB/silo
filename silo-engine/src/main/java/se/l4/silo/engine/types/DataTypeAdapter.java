package se.l4.silo.engine.types;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

public class DataTypeAdapter
	implements DataType
{
	private static final byte NULL = 0;
	private static final byte VALUE = 1;
	
	private final FieldType type;

	public DataTypeAdapter(FieldType type)
	{
		this.type = type;
	}
	
	public FieldType getType()
	{
		return type;
	}

	@Override
	public int compare(Object a, Object b)
	{
		if(a == b) return 0;

		if(a == MaxMin.MIN)
		{
			return -1;
		}
		else if(a == MaxMin.MAX)
		{
			return 1;
		}
		else if(b == MaxMin.MIN)
		{
			return 1;
		}
		else if(b == MaxMin.MAX)
		{
			return -1;
		}
		
		return type.compare(a, b);
	}

	@Override
	public int getMemory(Object obj)
	{
		return type.estimateMemory(obj);
	}
	
	private void write(Object obj, ExtendedDataOutputImpl out)
	{
		try
		{
			if(obj == null)
			{
				out.write(NULL);
			}
			else
			{
				out.write(VALUE);
				type.write(obj, out);
			}
		}
		catch(IOException e)
		{
			throw new IllegalStateException("Unable to write; " + e.getMessage(), e);
		}
	}

	@Override
	public void write(WriteBuffer buff, Object obj)
	{
		write(obj, new ExtendedDataOutputImpl(buff));
	}

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
	{
		ExtendedDataOutputImpl out = new ExtendedDataOutputImpl(buff);
		for(int i=0; i<len; i++)
		{
			write(obj[i], out);
		}
	}
	
	private Object read(ExtendedDataInputImpl in)
	{
		try
		{
			int tag = in.readByte();
			if(tag == NULL)
			{
				return null;
			}
			else
			{
				return type.read(in);
			}
		}
		catch(IOException e)
		{
			throw new IllegalStateException("Unable to read; " + e.getMessage(), e);
		}
	}

	@Override
	public Object read(ByteBuffer buff)
	{
		ExtendedDataInputImpl in = new ExtendedDataInputImpl(buff);
		return read(in);
	}

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
	{
		ExtendedDataInputImpl in = new ExtendedDataInputImpl(buff);
		for(int i=0; i<len; i++)
		{
			obj[i] = read(in);
		}
	}

	private static class ExtendedDataOutputImpl
		extends AbstractExtendedDataOutput
	{
		
		private final WriteBuffer buf;

		public ExtendedDataOutputImpl(WriteBuffer buf)
		{
			this.buf = buf;
		}
		
		@Override
		public void write(byte[] buffer, int offset, int length)
			throws IOException
		{
			buf.put(buffer, offset, length);
		}
		
		@Override
		public void write(int b)
			throws IOException
		{
			buf.put((byte) b);
		}

		@Override
		public void close()
			throws IOException
		{
		}
	}
	
	private static class ExtendedDataInputImpl
		extends AbstractExtendedDataInput
	{
		private ByteBuffer buf;

		public ExtendedDataInputImpl(ByteBuffer buf)
		{
			this.buf = buf;
		}
		
		@Override
		public int read() throws IOException
		{
			if(! buf.hasRemaining()) return -1;
			return buf.get() & 0xFF;
		}
		
		@Override
		public byte readByte()
			throws IOException
		{
			return buf.get();
		}
		
		@Override
		public void readFully(byte[] buffer, int offset, int length)
			throws IOException
		{
			if(length > buf.remaining())
			{
				throw new EOFException("Can not read " + length + " bytes, only have " + buf.remaining() + " left");
			}
			
			buf.get(buffer, offset, length);
		}
		
		@Override
		public int skipBytes(int n) throws IOException
		{
			buf.position(buf.position() + n);
			return n;
		}

		@Override
		public void close()
			throws IOException
		{
		}
	}
}
