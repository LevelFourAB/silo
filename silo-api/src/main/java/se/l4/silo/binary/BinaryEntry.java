package se.l4.silo.binary;

import se.l4.silo.FetchResult;
import se.l4.ylem.io.Bytes;

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
	 * Get the data of this item.
	 *
	 * @return
	 */
	Bytes getData();
}
