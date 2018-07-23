package se.l4.silo.engine;

import java.io.IOException;

import se.l4.commons.io.Bytes;

/**
 * Data storage for raw binary data. Binary data is always associated with an
 * identifier.
 *
 * @author Andreas Holstenson
 *
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
	void store(long id, Bytes bytes)
		throws IOException;

	/**
	 * Load bytes associated with a certain id.
	 *
	 * @param id
	 * @return
	 * @throws IOException
	 */
	Bytes get(long id)
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
