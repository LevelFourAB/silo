package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.function.Consumer;

import com.carrotsearch.hppc.LongArrayList;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.log.ChunkOutputStream;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;
import se.l4.silo.engine.types.ByteArrayFieldType;
import se.l4.silo.engine.types.LongArrayFieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.VersionedType;
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

	private static final int CHUNK_SIZE = 8192;

	private final MVStoreManager store;

	private final TransactionValue<MVMap<Long, long[]>> readonlyKeys;
	private final TransactionValue<MVMap<Long, byte[]>> readonlyChunks;

	private volatile MVMap<Long, long[]> keys;
	private volatile MVMap<Long, byte[]> chunks;

	public MVDataStorage(
		MVStoreManager store,
		TransactionSupport transactionSupport
	)
	{
		this.store = store;
		reopen();

		readonlyChunks = version -> chunks.openVersion(version);
		readonlyKeys = version -> keys.openVersion(version);

		transactionSupport.registerValue(readonlyChunks);
		transactionSupport.registerValue(readonlyKeys);
	}

	public void reopen()
	{
		keys = store.openMap("data.keys", LongFieldType.INSTANCE, VersionedType.singleVersion(new LongArrayFieldType()));
		chunks = store.openMap("data.chunks", LongFieldType.INSTANCE, ByteArrayFieldType.INSTANCE);
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
		Long lastId = keys.lastKey();
		long id = lastId == null ? 1 : lastId + 1;

		LongArrayList ids = new LongArrayList();

		try(OutputStream chunkOutput = new ChunkOutputStream(CHUNK_SIZE, (data, offset, length) -> {
			long nextId = nextInternalId();
			ids.add(nextId);

			byte[] buf = Arrays.copyOfRange(data, offset, offset + length);
			chunks.put(nextId, buf);

			if(log.isTraceEnabled())
			{
				log.trace("Store: Wrote " + nextId + " with data " + Base64.getEncoder().encodeToString(buf));
			}
		}))
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
