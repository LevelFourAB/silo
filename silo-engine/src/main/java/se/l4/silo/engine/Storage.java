package se.l4.silo.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryResult;
import se.l4.ylem.io.Bytes;

/**
 * Storage that can be used by {@link Entity entities}.
 *
 * @author Andreas Holstenson
 *
 */
public interface Storage
{
	/**
	 * Store some data in this storage.
	 *
	 * @param id
	 * @param bytes
	 * @return
	 */
	StoreResult store(Object id, Bytes bytes);

	/**
	 * Get some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	Bytes get(Object id);

	default StreamingInput getStructured(Object id)
	{
		Bytes bytes = get(id);
		if(bytes == null) return null;

		try
		{
			InputStream stream = bytes.asInputStream();

			int tag = stream.read();
			if(tag != 0)
			{
				throw new StorageException("Unknown storage version: " + tag + ", this version of Silo is either old or the data is corrupt");
			}

			return StreamingFormat.LEGACY_BINARY.createInput(stream);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not read data from storage during query engine update; " + e.getMessage(), e);
		}
	}

	/**
	 * Delete some data in this storage.
	 *
	 * @param id
	 * @return
	 */
	DeleteResult delete(Object id);

	/**
	 * Invoke a query engine by passing it a pre-built query.
	 *
	 * @param engine
	 * @param query
	 * @return
	 */
	<R> QueryFetchResult<QueryResult<R>> query(String engine, Object query, Function<Bytes, R> dataLoader);

	/**
	 * Stream everything stored in this storage.
	 *
	 * @return
	 */
	FetchResult<Bytes> stream();
}
