package se.l4.silo;

public interface StoreResult<ID, T>
{
	/**
	 * Get the identifier of the stored object.
	 *
	 * @return
	 */
	ID getId();
}
