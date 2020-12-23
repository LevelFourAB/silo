package se.l4.silo.engine.internal.tx;

import java.io.OutputStream;

import se.l4.silo.engine.TransactionExchange;
import se.l4.ylem.io.IOConsumer;

/**
 * Exchange to allow entities to interact with a transaction. Exchanges map
 * to a single transaction and create transactions as needed.
 */
public interface WriteableTransactionExchange
	extends TransactionExchange
{
	/**
	 * Get the version of this exchange.
	 *
	 * @return
	 */
	long getVersion();

	/**
	 * Store data for an entity.
	 *
	 * @param entity
	 * @param id
	 * @param bytes
	 * @return
	 */
	void store(String entity, Object id, IOConsumer<OutputStream> generator);

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
	void index(String entity, String index, Object id, IOConsumer<OutputStream> generator);
}
