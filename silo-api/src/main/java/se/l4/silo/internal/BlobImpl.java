package se.l4.silo.internal;

import java.io.IOException;
import java.io.InputStream;

import se.l4.silo.Blob;
import se.l4.silo.StorageException;
import se.l4.ylem.io.IOSupplier;

/**
 * Implementation of {@link Blob}.
 */
public class BlobImpl<ID>
	implements Blob<ID>
{
	private final ID id;
	private final IOSupplier<InputStream> supplier;

	public BlobImpl(
		ID id,
		IOSupplier<InputStream> supplier
	)
	{
		this.id = id;
		this.supplier = supplier;
	}

	@Override
	public ID getId()
	{
		return id;
	}

	@Override
	public InputStream openStream()
	{
		try
		{
			return supplier.get();
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to open stream; " + e.getMessage(), e);
		}
	}
}
