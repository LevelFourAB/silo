package se.l4.silo.engine.internal.log;

import se.l4.commons.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.internal.StorageEngine;

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
	 * @param bytes
	 *   the data to store
	 * @return
	 *   result of the store operation
	 */
	StoreResult store(long tx, String entity, Object id, Bytes bytes);

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
	DeleteResult delete(long tx, String entity, Object id);

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
