package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.EntityRef;
import se.l4.silo.StorageException;
import se.l4.silo.Transactions;
import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalEntity;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.Maintenance;
import se.l4.silo.engine.internal.tx.TransactionSupport;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;

/**
 * Implementation of {@link LocalSilo}. This implementation is built as a layer
 * on top of {@link StorageEngine} and creates instances of {@link LocalEntity}
 * that delegate their work to {@link Storage} instances.
 */
public class LocalSiloImpl
	implements LocalSilo
{
	private final StorageEngine storageEngine;
	private final TransactionSupport transactionSupport;
	private final ImmutableMap<String, LocalEntity<?, ?>> entities;

	private final MaintenanceImpl maintenance;

	LocalSiloImpl(
		StorageEngine storageEngine,
		ImmutableMap<String, LocalEntity<?, ?>> entities
	)
	{
		this.storageEngine = storageEngine;
		this.entities = entities;

		this.transactionSupport = storageEngine.getTransactionSupport();
		this.maintenance = new MaintenanceImpl(storageEngine);
	}

	@Override
	public void close()
	{
		try
		{
			storageEngine.close();
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to close; " + e.getMessage(), e);
		}
	}

	@Override
	public boolean hasEntity(String entityName)
	{
		return entities.containsKey(entityName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID, T> LocalEntity<ID, T> entity(EntityRef<ID, T> ref)
	{
		Objects.requireNonNull(ref);

		LocalEntity<?, ?> entity = entities.get(ref.getName());
		if(entity == null)
		{
			throw new StorageException("The entity `" + ref.getName() + "` does not exist");
		}

		// TODO: Validate the type before returning
		return (LocalEntity<ID, T>) entity;
	}

	@Override
	public Flux<LocalEntity<?, ?>> entities()
	{
		return Flux.fromIterable(entities);
	}

	@Override
	public Transactions transactions()
	{
		return transactionSupport;
	}

	@Override
	public Maintenance maintenance()
	{
		return maintenance;
	}

	public static class BuilderImpl
		implements Builder
	{
		private final LogBuilder logBuilder;
		private final Path dataPath;

		@SuppressWarnings("rawtypes")
		private final ImmutableList<EntityDefinition> entityDefinitions;

		private final EngineConfig config;
		private final Vibe vibe;

		@SuppressWarnings("rawtypes")
		public BuilderImpl(
			LogBuilder logBuilder,
			Path dataPath,

			EngineConfig config,
			ImmutableList<EntityDefinition> entityDefinitions,

			Vibe vibe
		)
		{
			this.logBuilder = logBuilder;
			this.dataPath = dataPath;
			this.config = config;
			this.vibe = vibe;
			this.entityDefinitions = entityDefinitions;
		}

		@Override
		public LocalSilo.Builder withVibe(Vibe vibe, String... path)
		{
			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				entityDefinitions,
				vibe.scope(path)
			);
		}

		@Override
		public Builder addEntity(EntityDefinition<?, ?> definition)
		{
			Objects.requireNonNull(definition);

			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				entityDefinitions.newWith(definition),
				vibe
			);
		}

		@Override
		public Builder addEntity(Buildable<? extends EntityDefinition<?, ?>> buildable)
		{
			return addEntity(buildable.build());
		}

		@Override
		public Builder addEntities(
			Iterable<? extends EntityDefinition<?, ?>> definitions
		)
		{
			Objects.requireNonNull(definitions);

			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				entityDefinitions.newWithAll(definitions),
				vibe
			);
		}

		@Override
		public Builder withCacheSize(int cacheSizeInMb)
		{
			return new BuilderImpl(
				logBuilder,
				dataPath,
				config.setCacheSizeInMb(cacheSizeInMb),
				entityDefinitions,
				vibe
			);
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Mono<LocalSilo> start()
		{
			return Mono.fromSupplier(() -> {
				// Create the storage engine
				StorageEngine storageEngine = new StorageEngine(
					vibe,
					logBuilder,
					dataPath,
					config,
					entityDefinitions
				);

				/*
				 * Map up all of the entities, creating storages in the engine
				 * as needed.
				 */
				ImmutableMap<String, LocalEntity<?, ?>> entities = (ImmutableMap) entityDefinitions.collect(def -> {
					return new EntityImpl(
						def.getName(),
						def.getIdSupplier(),
						storageEngine.createStorage(def.getName(), def.getCodec())
							.addIndexes(def.getIndexes())
							.build()
					);
				}).toMap(v -> v.getName(), v -> v).toImmutable();

				return new LocalSiloImpl(
					storageEngine,
					entities
				);
			});
		}
	}
}
