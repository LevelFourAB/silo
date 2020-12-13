package se.l4.silo;

import java.io.InputStream;

import se.l4.silo.internal.BlobImpl;
import se.l4.ylem.io.Bytes;
import se.l4.ylem.io.IOSupplier;

/**
 * Binary Large OBject for simplifying storing binary data.
 */
public interface Blob<ID>
{
	/**
	 * Get the identifier of this blob.
	 *
	 * @return
	 */
	ID getId();

	/**
	 * Open a stream with the data of this blob. For blobs fetched from an
	 * {@link Entity} this will open data as it exists within the current
	 * {@link Transaction}. If called outside an existing transaction this may
	 * error if the blob has been deleted.
	 *
	 * @return
	 *   stream with data
	 * @throws SiloException
	 *   if unable to open the data
	 */
	InputStream openStream();

	/**
	 * Create an instance.
	 *
	 * @param <ID>
	 *   type of id
	 * @param id
	 *   the identifier of the blob
	 * @param stream
	 *   supplier for getting the binary data of the blob
	 * @return
	 */
	static <ID> Blob<ID> create(ID id, IOSupplier<InputStream> stream)
	{
		return new BlobImpl<ID>(id, stream);
	}

	/**
	 * Create an instance.
	 *
	 * @param <ID>
	 *   type of id
	 * @param id
	 *   the identifier of the blob
	 * @param bytes
	 *   instance of bytes
	 * @return
	 */
	static <ID> Blob<ID> create(ID id, Bytes bytes)
	{
		return create(id, bytes::asInputStream);
	}
}
