package se.l4.silo.engine;

import java.io.Closeable;

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
	 * Acquire a version handle for this store.
	 *
	 * @return
	 */
	VersionHandle acquireVersionHandle();

	/**
	 * Register a commit action.
	 *
	 * @param action
	 */
	void registerCommitAction(CommitAction action);

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
	 * Handle to a specific version of the store. Handles are used make sure a
	 * certain version of the store is kept available.
	 */
	interface VersionHandle
	{
		/**
		 * Get the version being held.
		 *
		 * @return
		 */
		long getVersion();

		/**
		 * Release this version allowing it to be removed.
		 */
		void release();
	}

	/**
	 * Action tied to a commit of the store.
	 */
	interface CommitAction
	{
		/**
		 * Hook to call before a commit occurs.
		 */
		void preCommit();

		/**
		 * Hook to call after a commit occurs.
		 */
		void afterCommit();
	}
}
