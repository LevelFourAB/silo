package se.l4.silo.binary;

import se.l4.commons.io.Bytes;
import se.l4.silo.FetchResult;

/**
 * Item as returned via {@link FetchResult}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface BinaryEntry
{
	/**
	 * Get the identifier.
	 * 
	 * @return
	 */
	Object getId();
	
	/**
	 * Get the version if available.
	 * 
	 * @return
	 */
	Object getVersion();
	
	/**
	 * Get the data of this item.
	 * 
	 * @return
	 */
	Bytes getData();
}
