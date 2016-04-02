package se.l4.silo.engine.internal.mvstore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;

import se.l4.silo.StorageException;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.types.FieldType;

/**
 * Utility for managing {@link MVStoreManager}s that are shared.
 * 
 * @author Andreas Holstenson
 *
 */
public class SharedStorages
{
	private final Lock fetchLock;
	private final Map<String, ManagerInfo> storages;
	private final Path root;
	
	public SharedStorages(Path root)
	{
		this.root = root;
		fetchLock = new ReentrantLock();
		storages = new HashMap<>();
	}
	
	public MVStoreManager get(Path path)
	{
		path = root.resolve(path).normalize();
		String key = path.toString();
		
		fetchLock.lock();
		try
		{
			ManagerInfo info = storages.get(key);
			if(info != null)
			{
				return info.get();
			}
			
			// Create a new storage for the given path
			Files.createDirectories(path.getParent());
			MVStoreManagerImpl manager = new MVStoreManagerImpl(new MVStore.Builder()
				.fileName(path.toString())
				.compress());
			
			info = new ManagerInfo(key, manager);
			storages.put(key, info);
			return info.get();
		}
		catch(IOException e)
		{
			throw new StorageException("Could not create MVStoreManager; " + e.getMessage(), e);
		}
		finally
		{
			fetchLock.unlock();
		}
	}
	
	/**
	 * Controller for the created instances of {@link MVStoreManager}.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	private class ManagerInfo
	{
		private final String key;
		private final MVStoreManager manager;
		
		private int count;
		
		public ManagerInfo(String key, MVStoreManager actualManager)
		{
			this.key = key;
			manager = actualManager;
		}
		
		public SharedManager get()
		{
			SharedManager sm = new SharedManager(this);
			fetchLock.lock();
			try
			{
				count++;
			}
			finally
			{
				fetchLock.unlock();
			}
			
			return sm;
		}
		
		public void release(SharedManager sharedManager)
			throws IOException
		{
			fetchLock.lock();
			try
			{
				if(--count <= 0)
				{
					manager.close();
					storages.remove(key);
				}
			}
			finally
			{
				fetchLock.unlock();
			}
		}
	}
	
	private static class SharedManager
		implements MVStoreManager
		
	{
		private final ManagerInfo control;
		private boolean open;

		public SharedManager(ManagerInfo control)
		{
			this.control = control;
			
			open = true;
		}
		
		@Override
		public void close()
			throws IOException
		{
			synchronized(this)
			{
				if(! open) return;
				
				open = false;
				control.release(this);
			}
		}

		@Override
		public <K, V> MVMap<K, V> openMap(String name, FieldType<K> key, FieldType<V> value)
		{
			return control.manager.openMap(name, key, value);
		}

		@Override
		public <K, V> MVMap<K, V> openMap(String name, Builder<K, V> builder)
		{
			return control.manager.openMap(name, builder);
		}

		@Override
		public Snapshot createSnapshot()
		{
			throw new UnsupportedOperationException("Shared stores can not create snapshots");
		}

		@Override
		public void installSnapshot(Snapshot snapshot)
			throws IOException
		{
			throw new UnsupportedOperationException("Shared stores can not install snapshots");
		}

		@Override
		public void recreate() throws IOException
		{
			throw new UnsupportedOperationException("Shared stores can not be recreated");
		}
	}
}