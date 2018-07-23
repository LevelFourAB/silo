package se.l4.silo.engine;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import se.l4.silo.query.Query;

/**
 * Encounter with a query, contains information that can be used by a
 * {@link QueryEngine} to return results.
 *
 * @author Andreas Holstenson
 *
 */
public interface QueryEncounter<T>
{
	/**
	 * Get the data that describes the query.
	 *
	 * @return
	 */
	T getData();

	/**
	 * Load the given object.
	 *
	 * @param id
	 * @return
	 */
	Object load(long id);

	/**
	 * Indicate that the given data should be returned as a result for this
	 * query.
	 *
	 * @param id
	 */
	void receive(long id);

	/**
	 * Indicate that the given data should be returned as a result for this
	 * query. This method allows the caller to send some extra metadata with
	 * the result.
	 *
	 * @param id
	 */
	void receive(long id, Consumer<BiConsumer<String, Object>> metadataCreator);

	/**
	 * Add some metadata that will be available to the {@link Query} when
	 * creating the result of this encounter.
	 *
	 * @param key
	 * @param value
	 */
	void addMetadata(String key, Object value);

	/**
	 * Set the metadata about offset, limit and total number of hits found
	 * for this query.
	 *
	 * @param offset
	 * @param limit
	 * @param totalHits
	 */
	void setMetadata(long offset, long limit, long totalHits);
}
