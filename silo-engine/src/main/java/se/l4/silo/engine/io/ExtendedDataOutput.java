package se.l4.silo.engine.io;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;

import se.l4.ylem.io.Bytes;

/**
 * Extension to {@link DataOutput} for allowing the writing of some extra
 * types.
 */
public interface ExtendedDataOutput
	extends DataOutput, Closeable
{
	/**
	 * Write a positive integer encoding it using a variable encoding.
	 *
	 * @param l
	 * @throws IOException
	 */
	void writeVInt(int i)
		throws IOException;

	/**
	 * Write a positive integer encoding it using a variable encoding.
	 *
	 * @param l
	 * @throws IOException
	 */
	void writeVLong(long l)
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
	 * Write some bytes to the output.
	 *
	 * @param bytes
	 * @throws IOException
	 */
	void writeBytes(Bytes bytes)
		throws IOException;
}
