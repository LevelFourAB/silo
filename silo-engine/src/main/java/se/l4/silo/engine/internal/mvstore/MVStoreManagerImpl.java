package se.l4.silo.engine.internal.mvstore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;
import org.h2.store.fs.FileChannelInputStream;

import com.google.common.io.ByteStreams;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.FieldType;

/**
 * Implementation of {@link MVStoreManager}.
 * 
 * @author Andreas Holstenson
 *
 */
public class MVStoreManagerImpl
	implements MVStoreManager
{
	/** Target fill rate for MVStore */
	private static final int TARGET_RATE = 95;
	/** Data size to move when compacting */
	private static final int DATA_SIZE = 16 * 1024 * 1024;
	/** The time to spend compacting when closing the store */
	private static final long CLOSE_COMPACT_TIME = 200;

	private final MVStore.Builder builder;
	
	private volatile MVStore store;
	private AtomicLong snapshotsOpen;

	public MVStoreManagerImpl(MVStore.Builder builder)
	{
		this.builder = builder;
		store = builder.open();
		
		snapshotsOpen = new AtomicLong();
	}
	
	public MVStore getStore()
	{
		return store;
	}

	@Override
	public <K, V> MVMap<K, V> openMap(String name, FieldType<K> key, FieldType<V> value)
	{
		return store.openMap(name, new MVMap.Builder<K, V>()
			.keyType(new DataTypeAdapter(key))
			.valueType(new DataTypeAdapter(value)));
	}
	
	@Override
	public <K, V> MVMap<K, V> openMap(String name, Builder<K, V> builder)
	{
		return store.openMap(name, builder);
	}

	@Override
	public Snapshot createSnapshot()
	{
		snapshotsOpen.incrementAndGet();
		store.commit();
		store.setReuseSpace(false);
		return new Snapshot()
		{
			private boolean closed = false;
			
			@Override
			public InputStream asStream()
				throws IOException
			{
				if(store.getReuseSpace())
				{
					throw new IOException("Can not use this snapshot, the underlying storage might be in an incosistent state");
				}
				
				if(closed)
				{
					throw new IOException("This snapshot has already been closed");
				}
				
				return new FileChannelInputStream(
					store.getFileStore().getFile(),
					false
				);
			}
			
			@Override
			public void close()
				throws IOException
			{
				if(closed) return;
				
				closed = true;
				if(snapshotsOpen.decrementAndGet() == 0)
				{
					store.setReuseSpace(true);
				}
			}
		};
	}
	
	@Override
	public void installSnapshot(Snapshot snapshot)
		throws IOException
	{
		if(snapshotsOpen.get() > 0)
		{
			throw new IOException("Can not install snapshot as this store has an open snapshot");
		}
		
		// Get the file name we are replacing
		String fileName = store.getFileStore().getFileName();
		if(fileName.startsWith("nio:"))
		{
			fileName = fileName.substring(4);
		}
		
		// Close the existing store
		store.closeImmediately();
		
		try(InputStream in = snapshot.asStream(); OutputStream out = new FileOutputStream(fileName))
		{
			ByteStreams.copy(in, out);
		}
		
		store = builder.open();
	}
	
	@Override
	public void recreate() throws IOException
	{
		if(snapshotsOpen.get() > 0)
		{
			throw new IOException("Can not recreate as this store has an open snapshot");
		}
		
		// Get the file name we are replacing
		String fileName = store.getFileStore().getFileName();
		
		// Close the existing store
		store.closeImmediately();
		
		// Delete the data
		Files.deleteIfExists(Paths.get(fileName));
		
		store = builder.open();
	}

	@Override
	public void close()
		throws IOException
	{
		store.close();
	}

	public void compact(long targetTime)
	{
		if(snapshotsOpen.get() > 0)
		{
			return;
		}
		
		// TODO: This should really be smarter
		
		int retentionTime = store.getRetentionTime();
		store.setRetentionTime(0);

		long start = System.currentTimeMillis();
		while(store.compact(TARGET_RATE, DATA_SIZE))
		{
			store.sync();
			store.compactMoveChunks(TARGET_RATE, DATA_SIZE);
			long now = System.currentTimeMillis();
			if(now - start > targetTime) break;
		}

		store.setRetentionTime(retentionTime);
	}
}
