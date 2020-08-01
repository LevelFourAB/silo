package se.l4.silo.engine.log;

import java.io.Closeable;
import java.io.IOException;

import se.l4.ylem.io.Bytes;

/**
 * Abstraction of a log that contains binary data. Logs are used to apply
 * operations to Silo in order. Logs can synchronized over several machines
 * or simply stored locally or even in memory.
 *
 * @author Andreas Holstenson
 *
 */
public interface Log
	extends Closeable
{
	/**
	 * Append some data to this log.
	 *
	 * @param bytes
	 */
	void append(Bytes bytes)
		throws IOException;
}
