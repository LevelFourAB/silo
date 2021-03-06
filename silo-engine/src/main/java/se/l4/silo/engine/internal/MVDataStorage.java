package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.log.ChunkOutputStream;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;
import se.l4.silo.engine.internal.types.ByteChunkFieldType;
import se.l4.silo.engine.internal.types.KeyLongType;
import se.l4.silo.engine.internal.types.LongArrayFieldType;
import se.l4.ylem.io.IOConsumer;

/**
 * Data storage that uses {@link MVStore} to store all of the data. This
 * storage will split the data into smaller pieces so that only a subset
 * of the data needs to be loaded into memory at the same time.
 *
 * @author Andreas Holstenson
 *
 */
public class MVDataStorage
	implements DataStorage
{
	private static final Logger log = LoggerFactory.getLogger(MVDataStorage.class);

	private static final int CHUNK_SIZE = 256 * 1024;

	private final TransactionValue<MVMap<Long, long[]>> readonlyKeys;
	private final TransactionValue<MVMap<Long, byte[]>> readonlyChunks;

	private final MVMap<Long, long[]> keys;
	private final MVMap<Long, byte[]> chunks;

	/**
	 * Buffer used for all stores.
	 */
	private final byte[] buffer;
	/**
	 * Lock used to protect stores. Ensures that only a single thread can
	 * store something in this storage at once.
	 */
	private final Lock storeLock;

	public MVDataStorage(
		String prefix,
		MVStoreManager store
	)
	{
		keys = store.openMap(prefix + ".keys", new MVMap.Builder<Long, long[]>()
			.keyType(KeyLongType.INSTANCE)
			.valueType(LongArrayFieldType.INSTANCE)
		);

		chunks = store.openMap(prefix + ".chunks", new MVMap.Builder<Long, byte[]>()
			.keyType(KeyLongType.INSTANCE)
			.valueType(ByteChunkFieldType.INSTANCE)
		);

		readonlyChunks = version -> chunks.openVersion(version);
		readonlyKeys = version -> keys.openVersion(version);

		storeLock = new ReentrantLock();
		buffer = new byte[CHUNK_SIZE];
	}

	@Override
	public void provideTransactionValues(
		Consumer<? super TransactionValue<?>> consumer
	)
	{
		consumer.accept(readonlyKeys);
		consumer.accept(readonlyChunks);
	}

	@Override
	public long store(IOConsumer<OutputStream> generator)
		throws IOException
	{
		storeLock.lock();
		try
		{
			Long lastId = keys.lastKey();
			long id = lastId == null ? 1 : lastId + 1;

			MutableLongList ids = new LongArrayList();

			ChunkOutputStream.Control control = (data, offset, length) -> {
				long nextId = nextInternalId();
				ids.add(nextId);

				byte[] buf = Arrays.copyOfRange(data, offset, offset + length);
				chunks.put(nextId, buf);

				if(log.isTraceEnabled())
				{
					log.trace("Store: Wrote " + nextId + " with data " + Base64.getEncoder().encodeToString(buf));
				}
			};

			try(OutputStream chunkOutput = new ChunkOutputStream(buffer, control))
			{
				// Ask the generator to write output
				generator.accept(chunkOutput);
			}

			if(log.isTraceEnabled())
			{
				log.trace("Store: Mapped " + id + " to " + Arrays.toString(ids.toArray()));
			}

			// Store the new ids
			keys.put(id, ids.toArray());

			return id;
		}
		finally
		{
			storeLock.unlock();
		}
	}

	@Override
	public InputStream get(WriteableTransactionExchange exchange, long id)
		throws IOException
	{
		MVMap<Long, long[]> keys = exchange == null ? this.keys : exchange.get(readonlyKeys);

		long[] ids = keys.get(id);

		if(log.isTraceEnabled())
		{
			log.trace("Get: Mapped " + id + " to " + Arrays.toString(ids));
		}

		if(ids == null) return null;

		return new SequenceInputStream(
			new ChunkedInputStreamEnumeration(
				exchange == null ? this.chunks : exchange.get(readonlyChunks),
				ids
			)
		);
	}

	@Override
	public void delete(long id) throws IOException
	{
		long[] ids = keys.get(id);
		if(ids == null) return;

		if(log.isTraceEnabled())
		{
			log.trace("Delete: Mapped " + id + " to " + Arrays.toString(ids));
		}

		for(long chunk : ids)
		{
			chunks.remove(chunk);
		}

		keys.remove(id);
	}

	long nextInternalId()
	{
		Long lastKey = chunks.lastKey();
		return lastKey == null ? 1 : lastKey + 1;
	}

	private class ChunkedInputStreamEnumeration
		implements Enumeration<InputStream>
	{
		private final MVMap<Long, byte[]> chunks;
		private final long[] ids;
		private int idx;

		public ChunkedInputStreamEnumeration(
			MVMap<Long, byte[]> chunks,
			long[] ids
		)
		{
			this.chunks = chunks;
			this.ids = ids;
			idx = 0;
		}

		@Override
		public boolean hasMoreElements()
		{
			return idx < ids.length;
		}

		@Override
		public InputStream nextElement()
		{
			long id = ids[idx++];
			byte[] data = chunks.get(id);

			if(log.isTraceEnabled())
			{
				log.trace("read " + id + " with data " + Base64.getEncoder().encodeToString(data));
			}

			return new ByteArrayInputStream(data);
		}
	}
}
