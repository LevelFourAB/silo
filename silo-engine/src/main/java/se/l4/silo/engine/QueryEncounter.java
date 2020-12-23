package se.l4.silo.engine;

import se.l4.silo.query.Query;

/**
 * Encounter with a query, contains information that can be used by a
 * {@link QueryEngine} to return results.
 */
public interface QueryEncounter<D extends Query<T, ?, ?>, T>
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
