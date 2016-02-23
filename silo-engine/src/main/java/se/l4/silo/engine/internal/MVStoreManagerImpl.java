package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;
import org.h2.store.fs.FileChannelInputStream;

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
	private final MVStore store;
	private volatile int snapshotsOpen;

	public MVStoreManagerImpl(MVStore store)
	{
		this.store = store;
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
		snapshotsOpen++;
		store.setReuseSpace(false);
		return new Snapshot()
		{
			
			@Override
			public InputStream asStream()
				throws IOException
			{
				if(store.getReuseSpace())
				{
					throw new IOException("Can not use this snapshot, the underlying storage might be in an incosistent state");
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
				if(--snapshotsOpen == 0)
				{
					store.setReuseSpace(true);
				}
			}
		};
	}

	@Override
	public void close()
		throws IOException
	{
		store.close();
	}
}
