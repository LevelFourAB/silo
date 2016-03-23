package se.l4.silo.engine;

import java.io.Closeable;
import java.io.IOException;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import se.l4.silo.engine.types.FieldType;

/**
 * Manager of an instance of {@link MVStore}. This utility class helps with
 * {@link #openMap(String, FieldType, FieldType) getting MVMap instances}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface MVStoreManager
	extends Closeable
{
	/**
	 * Open a map from this store.
	 * 
	 * @param name
	 * @param key
	 * @param value
	 * @return
	 */
	<K, V> MVMap<K, V> openMap(String name, FieldType<K> key, FieldType<V> value);
	
	/**
	 * Open a map in this store.
	 * 
	 * @param name
	 * @param builder
	 * @return
	 */
	<K, V> MVMap<K, V> openMap(String name, MVMap.Builder<K, V> builder);
	
	/**
	 * Create a snapshot of this store. The snapshot temporarily disables
	 * automatic disk space reuse until the snapshot is closed.
	 * 
	 * <p>
	 * The snapshot <b>must</b> be closed when the caller is done working
	 * with it.
	 * 
	 * @return
	 */
	Snapshot createSnapshot();
	
	/**
	 * Install a snapshot into this store. This will remove all old data
	 * and reopen the {@link MVStore} instance. This will not reopen
	 * any {@link MVMap} instances, these need to be fetched from the manager
	 * again.
	 * 
	 * @param snapshot
	 */
	void installSnapshot(Snapshot snapshot)
		throws IOException;
	
	/**
	 * Destroy all the data in this store and recreate it as an empty storage.
	 * This will not reopen any {@link MVMap} instances, these need to be
	 * fetched from the manager again.
	 * 
	 * @throws IOException
	 */
	void recreate()
		throws IOException;
}
