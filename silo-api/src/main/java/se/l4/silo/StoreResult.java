package se.l4.silo;

public interface StoreResult
{
	/**
	 * Get the identifier of the stored object.
	 *
	 * @return
	 */
	Object getId();

	/**
	 * Get the size of the stream stored.
	 *
	 * @return
	 */
	long getSize();
}
