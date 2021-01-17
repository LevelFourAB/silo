package se.l4.silo.engine.types;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.engine.io.AbstractBinaryDataInput;
import se.l4.silo.engine.io.AbstractBinaryDataOutput;

public class DataTypeAdapter
	implements DataType
{
	private static final int NULL = 0;
	private static final int VALUE = 1;

	@SuppressWarnings("rawtypes")
	private final FieldType type;

	@SuppressWarnings("rawtypes")
	public DataTypeAdapter(FieldType type)
	{
		this.type = type;
	}

	@SuppressWarnings("rawtypes")
	public FieldType getType()
	{
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public int getMemory(Object obj)
	{
		return type.estimateMemory(obj);
	}

	@SuppressWarnings("unchecked")
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
			int tag = in.read();
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
		extends AbstractBinaryDataOutput
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
	}

	private static class ExtendedDataInputImpl
		extends AbstractBinaryDataInput
	{
		private ByteBuffer buf;

		public ExtendedDataInputImpl(ByteBuffer buf)
		{
			this.buf = buf;
		}

		@Override
		public int read()
			throws IOException
		{
			if(! buf.hasRemaining()) return -1;
			return buf.get() & 0xFF;
		}

		@Override
		public int read(byte[] buffer, int offset, int length)
			throws IOException
		{
			int len = Math.min(buf.remaining(), length);
			buf.get(buffer, offset, len);
			return len;
		}
	}
}
