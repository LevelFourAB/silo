package se.l4.silo.engine.index;

import se.l4.silo.engine.TransactionValue;
import se.l4.silo.index.Query;

/**
 * Encounter with a query, contains information that can be used by a
 * {@link IndexEngine} to return results.
 */
public interface IndexQueryEncounter<D extends Query<T, ?, ?>, T>
{
	/**
	 * Get the data of the query.
	 *
	 * @return
	 */
	D getQuery();

	/**
	 * Load the given object.
	 *
	 * @param id
	 * @return
	 */
	T load(long id);

	/**
	 * Get a value stored in this exchange.
	 *
	 * @param value
	 * @return
	 */
	<V> V get(TransactionValue<V> value);
}
