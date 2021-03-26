package se.l4.silo.engine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.collections.api.factory.Lists;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.CollectionRef;
import se.l4.silo.Silo;
import se.l4.silo.StorageException;
import se.l4.silo.engine.internal.LocalSiloImpl;
import se.l4.silo.engine.log.DirectApplyLog;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;

/**
 * Local instance of {@link Silo}.
 */
public interface LocalSilo
	extends Silo
{
	/**
	 * Get a collection.
	 *
	 * @param ref
	 *   reference to the collection
	 * @return
	 *   found collection
	 * @throws StorageException
	 *   if collection can not be found
	 */
	<ID, T> LocalCollection<ID, T> getCollection(CollectionRef<ID, T> ref);

	/**
	 * Get a collection.
	 *
	 * @param name
	 *   name
	 * @param idType
	 *   the type of ids used
	 * @param objectType
	 *   the type of object stored
	 * @return
	 *   found collection
	 * @throws StorageException
	 *   if collection can not be found
	 */
	default <ID, T> LocalCollection<ID, T> getCollection(String name, Class<ID> idType, Class<T> objectType)
	{
		return getCollection(CollectionRef.create(name, idType, objectType));
	}

	/**
	 * Get all of the collections.
	 *
	 * @return
	 */
	Flux<LocalCollection<?, ?>> collections();

	/**
	 * Close this instance.
	 */
	void close();

	/**
	 * Access maintenance utilities for this instance.
	 *
	 * @return
	 */
	Maintenance maintenance();

	/**
	 * Start creating a local instance of Silo. The built instance will use a
	 * {@link DirectApplyLog} so any operations will be applied directly by the
	 * calling thread.
	 *
	 * @param path
	 *   the directory where data will be stored
	 * @return
	 */
	public static Builder open(Path path)
	{
		return open(DirectApplyLog.builder(), path);
	}

	/**
	 * Start creating a local instance of Silo. See {@link #open(Path)} for
	 * details about how the instance will behave.
	 *
	 * @param path
	 *   the directory where data will be stored
	 * @return
	 */
	public static Builder open(File path)
	{
		return open(path.toPath());
	}

	/**
	 * Start creating a local instance of Silo. See {@link #open(Path)} for
	 * details about how the instance will behave.
	 *
	 * @param first
	 *   the directory where data will be stored
	 * @param more
	 *
	 * @return
	 */
	public static Builder open(String first, String... more)
	{
		return open(Paths.get(first, more));
	}

	/**
	 * Start creating a new instance using a specific log to apply operations.
	 *
	 * @param logBuilder
	 * @param path
	 * @return
	 */
	public static Builder open(LogBuilder logBuilder, Path path)
	{
		return new LocalSiloImpl.BuilderImpl(
			logBuilder,
			path,
			new EngineConfig(),
			Lists.immutable.empty(),
			null
		);
	}

	/**
	 * Builder for instances of {@link LocalSilo}.
	 */
	interface Builder
	{
		/**
		 * Set the {@link Vibe} instance to use. Setting this will enable
		 * reporting of health values.
		 *
		 * @param vibe
		 * @param path
		 * @return
		 */
		Builder withVibe(Vibe vibe, String... path);

		/**
		 * Set the size used to cache data in this instance. This controls the
		 * cache size of the main storage, but does not do anything for indexes.
		 *
		 * @param cacheSizeInMb
		 * @return
		 */
		Builder withCacheSize(int cacheSizeInMb);

		/**
		 * Add a collection that should be available.
		 *
		 * @param definition
		 * @return
		 */
		Builder addCollection(CollectionDef<?, ?> definition);

		/**
		 * Add a collection.
		 *
		 * @param buildable
		 * @return
		 */
		Builder addCollection(Buildable<? extends CollectionDef<?, ?>> buildable);

		/**
		 * Add multiple definitions to this instance.
		 *
		 * @param definitions
		 * @return
		 */
		Builder addCollections(Iterable<? extends CollectionDef<?, ?>> definitions);

		/**
		 * Return a mono that will start this instance.
		 *
		 * @return
		 */
		Mono<LocalSilo> start();
	}
}
