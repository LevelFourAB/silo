package se.l4.silo;

/**
 * Result of a deletion.
 */
public interface DeleteResult
{
	/**
	 * Get if something was actually deleted.
	 *
	 * @return
	 */
	boolean wasDeleted();
}
