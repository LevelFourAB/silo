package se.l4.silo.engine;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.mvstore.MVStore;

/**
 * Encounter for when a {@link QueryEngine} is being constructed.
 *
 */
public interface QueryEngineCreationEncounter
{
	/**
	 * Get the name of the engine being created.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the name of this engine unique to the system.
	 *
	 * @return
	 */
	String getUniqueName();

	/**
	 * Resolve a name against the data directory of the entity the query
	 * engine will belong to.
	 *
	 * @param name
	 * @return
	 */
	Path resolveDataFile(String name);

	/**
	 * Resolve a path against the data directory of the entity the query
	 * engine will belong to.
	 *
	 * @param path
	 * @return
	 */
	Path resolveDataFile(Path path);

	/**
	 * Get the data directory for this query engine. This will create the
	 * directory if it does not exist.
	 *
	 * @return
	 */
	Path getDataDirectory();

	/**
	 * Open a new MVStore with the given name. The name will be passed to
	 * {@link #resolveDataFile(String)} to find where the data should be
	 * stored.
	 *
	 * @param name
	 * @return
	 */
	MVStoreManager openMVStore(String name);

	/**
	 * Open a new shared MVStore for the entire storage. Any caller of this
	 * method with the same name will receive an {@link MVStoreManager}
	 * pointing to the same {@link MVStore}.
	 *
	 * <p>
	 * Any users of this store are required to close the {@link MVStoreManager}
	 * when they are done using it.
	 *
	 * @param name
	 * @return
	 */
	MVStoreManager openStorageWideMVStore(String name);

	/**
	 * Get an executor that can be used for background tasks.
	 *
	 * <p>
	 * When a query engine is closed it must stop any background tasks that
	 * are queued or running.
	 *
	 * @return
	 */
	ScheduledExecutorService getExecutor();
}
