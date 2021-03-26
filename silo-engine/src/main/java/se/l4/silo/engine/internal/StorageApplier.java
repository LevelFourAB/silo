package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface representing the operations that actually modify the underlying
 * storage.
 */
public interface StorageApplier
{
	/**
	 * Callback called when a specific transaction has been completely applied.
	 *
	 * @param id
	 */
	void transactionStart(long id);

	/**
	 * Store some data for the given collection and identifier.
	 *
	 * @param collection
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	void store(String collection, Object id, InputStream data)
		throws IOException;

	/**
	 * Delete data for the given collection and identifier.
	 *
	 * @param collection
	 * @param id
	 */
	void delete(String collection, Object id)
		throws IOException;

	/**
	 * Receive data used for an index in a collection.
	 *
	 * @param collection
	 * @param index
	 * @param id
	 * @param data
	 * @throws IOException
	 */
	void index(String collection, String index, Object id, InputStream data)
		throws IOException;

	/**
	 * Callback called when a specific transaction has been completely applied.
	 *
	 * @param id
	 * @param throwable
	 */
	void transactionComplete(long id, Throwable throwable);
}
