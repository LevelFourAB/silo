package se.l4.silo;

/**
 * Transaction in {@link Silo}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Transaction
{
	/**
	 * Rollback any changes made.
	 */
	void rollback();
	
	/**
	 * Commit any changes made.
	 */
	void commit();
}
