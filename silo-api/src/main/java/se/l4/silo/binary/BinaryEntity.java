package se.l4.silo.binary;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;

/**
 * Simple binary key-value entity. Can be used to associate an identifier
 * with a {@link Bytes binary data}.
 * 
 * @author Andreas Holstenson
 */
public interface BinaryEntity
{
	/**
	 * Get the name of the entity.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Get an object via its identifier.
	 * 
	 * @param id
	 * @param callback
	 */
	FetchResult<BinaryEntry> get(Object id);
	
	/**
	 * Store a new value for this entity.
	 * 
	 * @param id
	 * @param bytes
	 * @return
	 */
	StoreResult store(Object id, Bytes bytes);
	
	/**
	 * Delete an already stored value.
	 * 
	 * @param id
	 * @return
	 */
	DeleteResult delete(Object id);
}
