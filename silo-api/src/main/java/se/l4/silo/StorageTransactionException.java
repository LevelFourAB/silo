package se.l4.silo;

/**
 * Exception thrown when an issue with a {@link Transaction} has been detected.
 * When such an exception is encountered the transaction may be retried.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageTransactionException
	extends StorageException
{
	public StorageTransactionException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
