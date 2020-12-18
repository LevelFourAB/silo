package se.l4.silo.engine.internal.log;

import java.io.OutputStream;

import se.l4.silo.engine.internal.StorageEngine;
import se.l4.ylem.io.IOConsumer;

/**
 * Log creation for transaction support. Will store operations as entries in
 * a log that is applied by a {@link StorageEngine}.
 *
 * @author Andreas Holstenson
 *
 */
public interface TransactionLog
{
	/**
	 * Start a transaction. This will return an identifier
	 *
	 * @return
	 */
	long startTransaction();

	/**
	 * Store some data in this transaction.
	 *
	 * @param tx
	 *   transaction identifier
	 * @param entity
	 *   the named entity this is for
	 * @param id
	 *   the id to store as
	 * @param generator
	 *   generator of the data
	 * @return
	 *   result of the store operation
	 */
	void store(long tx, String entity, Object id, IOConsumer<OutputStream> generator);

	/**
	 * Remove some data in this transaction.
	 *
	 * @param tx
	 *   transaction identifier
	 * @param entity
	 *   the named entity this is for
	 * @param id
	 *   the id to delete
	 */
	void delete(long tx, String entity, Object id);

	/**
	 * Store some index data in this transaction.
	 *
	 * @param tx
	 * @param entity
	 * @param id
	 * @param index
	 * @param generator
	 */
	void storeIndex(long tx, String entity, String index, Object id, IOConsumer<OutputStream> generator);

	/**
	 * Commit the given transaction.
	 *
	 * @param tx
	 */
	void commitTransaction(long tx);

	/**
	 * Rollback the given transaction.
	 *
	 * @param tx
	 */
	void rollbackTransaction(long tx);
}
