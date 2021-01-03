package se.l4.silo.engine;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;

import org.eclipse.collections.api.factory.Lists;

import reactor.core.publisher.Mono;
import se.l4.silo.EntityRef;
import se.l4.silo.Silo;
import se.l4.silo.engine.internal.LocalSiloBuilder;
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
	 * Get an entity.
	 *
	 * @param entityName
	 * @param ref
	 */
	<ID, T> LocalEntity<ID, T> entity(EntityRef<ID, T> ref);

	/**
	 * Get an entity.
	 *
	 * @param entityName
	 * @param type
	 */
	default <ID, T> LocalEntity<ID, T> entity(String name, Class<ID> idType, Class<T> objectType)
	{
		return entity(EntityRef.create(name, idType, objectType));
	}

	/**
	 * Close this instance.
	 */
	void close();

	/**
	 * Create a snapshot of this instance.
	 *
	 * @return
	 */
	Snapshot createSnapshot();

	/**
	 * Compact the storage.
	 *
	 * @param maxTime
	 *   the maximum time to spend compacting
	 */
	void compact(Duration maxTime);

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
	 * Start creating a new instance using a specific log to apply operations.
	 *
	 * @param logBuilder
	 * @param path
	 * @return
	 */
	public static Builder open(LogBuilder logBuilder, Path path)
	{
		return new LocalSiloBuilder(
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
		 * Add an entity that should be available.
		 *
		 * @param definition
		 * @return
		 */
		Builder addEntity(EntityDefinition<?, ?> definition);

		/**
		 * Add multiple definitions to this instance.
		 *
		 * @param definitions
		 * @return
		 */
		Builder addEntities(Iterable<? extends EntityDefinition<?, ?>> definitions);

		/**
		 * Return a mono that will start this instance.
		 *
		 * @return
		 */
		Mono<LocalSilo> start();
	}
}
