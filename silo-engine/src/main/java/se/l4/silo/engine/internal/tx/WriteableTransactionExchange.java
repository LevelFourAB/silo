package se.l4.silo.engine.internal.tx;

import java.io.OutputStream;

import se.l4.silo.engine.TransactionExchange;
import se.l4.ylem.io.IOConsumer;

/**
 * Exchange to allow collections to interact with a transaction. Exchanges map
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
	 * Store data for a collection.
	 *
	 * @param collection
	 * @param id
	 * @param bytes
	 * @return
	 */
	void store(String collection, Object id, IOConsumer<OutputStream> generator);

	/**
	 * Delete data associated with a collection.
	 *
	 * @param collection
	 * @param id
	 * @return
	 */
	void delete(String collection, Object id);

	/**
	 * Store index data for a collection.
	 *
	 * @param collection
	 * @param index
	 * @param id
	 * @param bytes
	 */
	void index(String collection, String index, Object id, IOConsumer<OutputStream> generator);
}
