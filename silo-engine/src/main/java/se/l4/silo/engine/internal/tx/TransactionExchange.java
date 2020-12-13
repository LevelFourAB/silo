package se.l4.silo.engine.internal.tx;

import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;
import se.l4.ylem.io.Bytes;

/**
 * Exchange to allow entities to interact with a transaction. Exchanges map
 * to a single transaction and create transactions as needed.
 */
public interface TransactionExchange
{
	/**
	 * Rollback any changes made.
	 */
	void rollback();

	/**
	 * Commit any changes made.
	 */
	void commit();

	StoreResult store(String entity, Object id, Bytes bytes);

	DeleteResult delete(String entity, Object id);

	void index(String entity, String index, Object id, Bytes bytes);
}
