package se.l4.silo.engine.io;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link DataInput} with extended methods for reading some more specific
 * but common types of data.
 */
public interface BinaryDataInput
{
	/**
	 * Read a single byte from the input.
	 */
	int read()
		throws IOException;

	/**
	 * Read bytes into the given buffer.
	 *
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	int read(byte[] buffer, int offset, int length)
		throws IOException;

	/**
	 * Read a boolean from the input.
	 *
	 * @return
	 * @throws IOException
	 */
	boolean readBoolean()
		throws IOException;

	/**
	 * Read an integer that has been encoded as a variable int.
	 *
	 * @see BinaryDataOutput#writeVInt(int)
	 */
	int readVInt()
		throws IOException;

	/**
	 * Read an integer.
	 *
	 * @see BinaryDataOutput#writeInt(int)
	 */
	int readInt()
		throws IOException;

	/**
	 * Read a long that has been encoded as a variable long.
	 *
	 * @see BinaryDataOutput#writeLong(long)
	 */
	long readVLong()
		throws IOException;

	/**
	 * Read a long.
	 *
	 * @return
	 * @throws IOException
	 * @see BinaryDataOutput#writeLong(long)
	 */
	long readLong()
		throws IOException;

	/**
	 * Read a float.
	 *
	 * @return
	 * @throws IOException
	 */
	float readFloat()
		throws IOException;

	/**
	 * Read a double.
	 *
	 * @return
	 * @throws IOException
	 */
	double readDouble()
		throws IOException;

	/**
	 * Read a string that was written in a compact UTF-8 format.
	 *
	 * @see BinaryDataOutput#writeString(String)
	 */
	String readString()
		throws IOException;

	/**
	 * Read an identifier.
	 *
	 * @see BinaryDataOutput#writeId(Object)
	 */
	Object readId()
		throws IOException;

	/**
	 * Read a byte array from this input.
	 *
	 * @return
	 * @throws IOException
	 */
	byte[] readByteArray()
		throws IOException;

	/**
	 * Create an instance for the given stream.
	 *
	 * @param in
	 * @return
	 */
	static BinaryDataInput forStream(InputStream in)
	{
		return new AbstractBinaryDataInput()
		{
			@Override
			public int read()
				throws IOException
			{
				return in.read();
			}

			@Override
			public int read(byte[] buffer, int offset, int length)
				throws IOException
			{
				return in.read(buffer, offset, length);
			}
		};
	}
}
