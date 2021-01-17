package se.l4.silo.engine.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import org.h2.mvstore.WriteBuffer;

/**
 * Extension to {@link DataOutput} for allowing the writing of some extra
 * types.
 */
public interface BinaryDataOutput
{
	/**
	 * Write a single byte to the output.
	 */
	void write(int b)
		throws IOException;

	/**
	 * Write several bytes to the output.
	 *
	 * @param data
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
	void write(byte[] data, int offset, int length)
		throws IOException;

	/**
	 * Write a boolean to the output.
	 *
	 * @param b
	 * @throws IOException
	 */
	void writeBoolean(boolean b)
		throws IOException;

	/**
	 * Write a float to the output.
	 *
	 * @param f
	 * @throws IOException
	 */
	void writeFloat(float f)
		throws IOException;

	/**
	 * Write a double to the output.
	 *
	 * @param d
	 * @throws IOException
	 */
	void writeDouble(double d)
		throws IOException;

	/**
	 * Write a positive integer, encoding it using a variable encoding.
	 *
	 * @param i
	 * @throws IOException
	 */
	void writeVInt(int i)
		throws IOException;

	/**
	 * Write an integer to the output.
	 *
	 * @param i
	 * @param IOException
	 */
	void writeInt(int i)
		throws IOException;

	/**
	 * Write a positive long, encoding it using a variable encoding.
	 *
	 * @param l
	 * @throws IOException
	 */
	void writeVLong(long l)
		throws IOException;

	/**
	 * Write a long to the output.
	 *
	 * @param l
	 * @throws IOException
	 */
	void writeLong(long l)
		throws IOException;

	/**
	 * Write a string as UTF-8 to the output.
	 *
	 * @param string
	 * @throws IOException
	 */
	void writeString(String string)
		throws IOException;

	/**
	 * Write an identifier.
	 *
	 * @param object
	 * @throws IOException
	 */
	void writeId(Object object)
		throws IOException;

	/**
	 * Write a byte array to the output.
	 *
	 * @param data
	 * @throws IOException
	 */
	void writeByteArray(byte[] data)
		throws IOException;

	/**
	 * Write a byte array to the output.
	 *
	 * @param data
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
	void writeByteArray(byte[] data, int offset, int length)
		throws IOException;

	static BinaryDataOutput forStream(OutputStream out)
	{
		return new AbstractBinaryDataOutput()
		{
			@Override
			public void write(int b)
				throws IOException
			{
				out.write(b);
			}

			@Override
			public void write(byte[] data, int offset, int length)
				throws IOException
			{
				out.write(data, offset, length);
			}
		};
	}

	static BinaryDataOutput forBuffer(WriteBuffer buf)
	{
		return new AbstractBinaryDataOutput()
		{
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
		};
	}
}
