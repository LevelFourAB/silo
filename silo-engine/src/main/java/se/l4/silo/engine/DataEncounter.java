package se.l4.silo.engine;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.ylem.io.Bytes;

public interface DataEncounter
{
	/**
	 * Get the binary data of this encounter.
	 *
	 * @return
	 */
	Bytes getData();

	/**
	 * Get as structured data.
	 *
	 * @return
	 */
	StreamingInput asStructured();

	/**
	 * Get a {@link Map} with values of the given keys.
	 *
	 * @param keys
	 * @return
	 */
	Map<String, Object> findStructuredKeys(Collection<String> keys);

	/**
	 * Get all of the values associated with the given keys.
	 *
	 * @param keys
	 * @param receiver
	 */
	void findStructuredKeys(Collection<String> keys, BiConsumer<String, Object> receiver);

	/**
	 * Get an array with the values of the given keys.
	 *
	 * @param keys
	 * @return
	 */
	Object[] getStructuredArray(String[] keys);

	/**
	 * Get an array with the value of the given keys, works the same as
	 * {@link #getStructuredArray(String[])} but allows for appending a few
	 * null values to the returned array.
	 *
	 * @param keys
	 * @param appendCount
	 * @return
	 */
	Object[] getStructuredArray(String[] keys, int appendCount);

	/**
	 * <strong>Expert</strong>: Get the main {@link Storage} instance for an entity.
	 *
	 * @param entity
	 * @return
	 */
	Storage getStorage(String entity);

	/**
	 * <strong>Expert</strong>: Get a {@link Storage} instance for an entity.
	 *
	 * @param entity
	 * @param name
	 * @return
	 */
	Storage getStorage(String entity, String name);

}
