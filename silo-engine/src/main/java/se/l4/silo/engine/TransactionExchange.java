package se.l4.silo.engine;

/**
 * Representation of the current transaction.
 */
public interface TransactionExchange
{
	/**
	 * Get a value stored in this exchange.
	 *
	 * @param value
	 * @return
	 */
	<V> V get(TransactionValue<V> value);
}
