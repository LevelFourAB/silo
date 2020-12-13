package se.l4.silo.search;

import se.l4.silo.StorageException;

public class SearchIndexException
	extends StorageException
{
	public SearchIndexException()
	{
	}

	public SearchIndexException(String message)
	{
		super(message);
	}

	public SearchIndexException(Throwable cause)
	{
		super(cause);
	}

	public SearchIndexException(
		String message,
		Throwable cause
	)
	{
		super(message, cause);
	}

	public SearchIndexException(
		String message,
		Throwable cause,
		boolean enableSuppression,
		boolean writableStackTrace
	)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
