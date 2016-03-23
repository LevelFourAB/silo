package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Enumeration;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import com.carrotsearch.hppc.LongArrayList;
import com.google.common.annotations.VisibleForTesting;

import se.l4.aurochs.core.io.ByteArrayConsumer;
import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.types.ByteArrayFieldType;
import se.l4.silo.engine.types.LongArrayFieldType;
import se.l4.silo.engine.types.LongFieldType;

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
	private static final int CHUNK_SIZE = 8192;
	
	private final MVStoreManager store;
	
	private volatile MVMap<Long, long[]> keys;
	private volatile MVMap<Long, byte[]> chunks;

	public MVDataStorage(MVStoreManager store)
	{
		this.store = store;
		reopen();
	}
	
	public void reopen()
	{
		keys = store.openMap("data.keys", LongFieldType.INSTANCE, new LongArrayFieldType());
		chunks = store.openMap("data.chunks", LongFieldType.INSTANCE, ByteArrayFieldType.INSTANCE);
	}

	@Override
	public void store(long id, Bytes bytes)
		throws IOException
	{
		LongArrayList ids = new LongArrayList();
		bytes.asChunks(CHUNK_SIZE, (data, offset, length) -> {
			long nextId = nextInternalId();
			ids.add(nextId);
			
			chunks.put(nextId, Arrays.copyOfRange(data, offset, offset + length));
		});
		
		keys.put(id, ids.toArray());
	}

	@Override
	public Bytes get(long id)
		throws IOException
	{
		long[] ids = keys.get(id);
		if(ids == null) return null;
		
		return new ChunkedBytes(ids);
	}

	@Override
	public void delete(long id) throws IOException
	{
		long[] ids = keys.get(id);
		if(ids == null) return;
		
		for(long chunk : ids)
		{
			chunks.remove(chunk);
		}
		
		keys.remove(id);
	}
	
	@VisibleForTesting
	long nextInternalId()
	{
		Long lastKey = chunks.lastKey();
		return lastKey == null ? 1 : lastKey + 1;
	}

	private class ChunkedBytes
		implements Bytes
	{
		private final long[] ids;

		public ChunkedBytes(long[] ids)
		{
			this.ids = ids;
		}

		@Override
		public InputStream asInputStream() throws IOException
		{
			return new SequenceInputStream(
				new ChunkedInputStreamEnumeration(ids)
			);
		}

		@Override
		public byte[] toByteArray() throws IOException
		{
			if(ids.length == 1)
			{
				return chunks.get(ids[0]);
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(CHUNK_SIZE * ids.length);
			for(long id : ids)
			{
				byte[] data = chunks.get(id);
				out.write(data);
			}
			return out.toByteArray();
		}
		
		@Override
		public void asChunks(ByteArrayConsumer consumer) throws IOException
		{
			for(long id : ids)
			{
				byte[] data = chunks.get(id);
				consumer.consume(data, 0, data.length);
			}
		}
	}
	
	private class ChunkedInputStreamEnumeration
		implements Enumeration<InputStream>
	{
		private final long[] ids;
		private int idx;
		
		public ChunkedInputStreamEnumeration(long[] ids)
		{
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
			return new ByteArrayInputStream(data);
		}
	}
}
