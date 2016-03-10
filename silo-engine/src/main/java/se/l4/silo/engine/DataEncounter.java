package se.l4.silo.engine;

import java.util.Collection;
import java.util.Map;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.serialization.format.StreamingInput;

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
}
