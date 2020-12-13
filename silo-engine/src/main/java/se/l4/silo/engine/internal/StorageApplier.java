package se.l4.silo.engine.internal;

import java.io.IOException;

import se.l4.ylem.io.Bytes;

/**
 * Interface representing the operations that acutally modify the underlying
 * storage.
 *
 * @author Andreas Holstenson
 *
 */
public interface StorageApplier
{
	/**
	 * Store some data for the given entity and identifier.
	 *
	 * @param entity
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	void store(String entity, Object id, Bytes data)
		throws IOException;

	/**
	 * Delete data for the given entity and identifier.
	 *
	 * @param entity
	 * @param id
	 */
	void delete(String entity, Object id)
		throws IOException;

	/**
	 *
	 * @param entity
	 * @param index
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	void index(String entity, String index, Object id, Bytes data)
		throws IOException;
}
