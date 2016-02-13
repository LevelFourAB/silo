package se.l4.silo;

/**
 * Exception thrown when a storage operation can not be completed.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageException
	extends RuntimeException
{

	public StorageException()
	{
		super();
	}

	public StorageException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public StorageException(String message)
	{
		super(message);
	}

	public StorageException(Throwable cause)
	{
		super(cause);
	}
	
}
