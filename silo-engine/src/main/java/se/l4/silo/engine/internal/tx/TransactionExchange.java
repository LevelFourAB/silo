package se.l4.silo.engine.internal.tx;

import se.l4.ylem.io.Bytes;

/**
 * Exchange to allow entities to interact with a transaction. Exchanges map
 * to a single transaction and create transactions as needed.
 */
public interface TransactionExchange
{
	/**
	 * Store data for an entity.
	 *
	 * @param entity
	 * @param id
	 * @param bytes
	 * @return
	 */
	void store(String entity, Object id, Bytes bytes);

	/**
	 * Delete data associated with an entity.
	 *
	 * @param entity
	 * @param id
	 * @return
	 */
	void delete(String entity, Object id);

	/**
	 * Store index data for an entity.
	 *
	 * @param entity
	 * @param index
	 * @param id
	 * @param bytes
	 */
	void index(String entity, String index, Object id, Bytes bytes);
}
