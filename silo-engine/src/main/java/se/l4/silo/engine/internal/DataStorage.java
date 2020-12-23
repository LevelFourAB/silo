package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;
import se.l4.ylem.io.IOConsumer;

/**
 * Data storage for raw binary data. Binary data is always associated with an
 * identifier.
 */
public interface DataStorage
{
	/**
	 * Store bytes associated with a certain id.
	 *
	 * @param id
	 * @param bytes
	 * @throws IOException
	 */
	long store(IOConsumer<OutputStream> out)
		throws IOException;

	/**
	 * Load bytes associated with a certain id.
	 *
	 * @param exchange
	 * @param id
	 * @return
	 * @throws IOException
	 */
	InputStream get(WriteableTransactionExchange exchange, long id)
		throws IOException;

	/**
	 * Delete data associated with a certain id.
	 *
	 * @param id
	 * @throws IOException
	 */
	void delete(long id)
		throws IOException;
}
