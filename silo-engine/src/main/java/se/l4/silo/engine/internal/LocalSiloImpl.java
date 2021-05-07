package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.CollectionRef;
import se.l4.silo.StorageException;
import se.l4.silo.Transactions;
import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.LocalCollection;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.Maintenance;
import se.l4.silo.engine.internal.tx.TransactionSupport;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;

/**
 * Implementation of {@link LocalSilo}. This implementation is built as a layer
 * on top of {@link StorageEngine} and creates instances of {@link LocalCollection}
 * that delegate their work to {@link Storage} instances.
 */
public class LocalSiloImpl
	implements LocalSilo
{
	private final StorageEngine storageEngine;
	private final TransactionSupport transactionSupport;
	private final ImmutableMap<String, LocalCollection<?, ?>> collections;

	private final MaintenanceImpl maintenance;

	LocalSiloImpl(
		StorageEngine storageEngine,
		ImmutableMap<String, LocalCollection<?, ?>> collections
	)
	{
		this.storageEngine = storageEngine;
		this.collections = collections;

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
	public boolean hasCollection(String name)
	{
		return collections.containsKey(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID, T> LocalCollection<ID, T> getCollection(CollectionRef<ID, T> ref)
	{
		Objects.requireNonNull(ref);

		LocalCollection<?, ?> collection = collections.get(ref.getName());
		if(collection == null)
		{
			throw new StorageException("The collection `" + ref.getName() + "` does not exist");
		}

		// TODO: Validate the type before returning
		return (LocalCollection<ID, T>) collection;
	}

	@Override
	public Flux<LocalCollection<?, ?>> collections()
	{
		return Flux.fromIterable(collections);
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
		private final ImmutableList<CollectionDef> collectionDefs;

		private final EngineConfig config;

		private final Vibe vibe;

		@SuppressWarnings("rawtypes")
		public BuilderImpl(
			LogBuilder logBuilder,
			Path dataPath,

			EngineConfig config,
			ImmutableList<CollectionDef> collectionDefs,

			Vibe vibe
		)
		{
			this.logBuilder = logBuilder;
			this.dataPath = dataPath;

			this.config = config;

			this.vibe = vibe;
			this.collectionDefs = collectionDefs;
		}

		@Override
		public LocalSilo.Builder withVibe(Vibe vibe, String... path)
		{
			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				collectionDefs,
				vibe.scope(path)
			);
		}

		@Override
		public Builder addCollection(CollectionDef<?, ?> definition)
		{
			Objects.requireNonNull(definition);

			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				collectionDefs.newWith(definition),
				vibe
			);
		}

		@Override
		public Builder addCollection(Buildable<? extends CollectionDef<?, ?>> buildable)
		{
			return addCollection(buildable.build());
		}

		@Override
		public Builder addCollections(
			Iterable<? extends CollectionDef<?, ?>> definitions
		)
		{
			Objects.requireNonNull(definitions);

			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				collectionDefs.newWithAll(definitions),
				vibe
			);
		}

		@Override
		public Builder withConfig(EngineConfig config)
		{
			return new BuilderImpl(
				logBuilder,
				dataPath,
				config,
				collectionDefs,
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
					collectionDefs
				);

				/*
				 * Map up all of the collections, creating storages in the engine
				 * as needed.
				 */
				ImmutableMap<String, LocalCollection<?, ?>> collections = (ImmutableMap) collectionDefs.collect(def -> {
					return new CollectionImpl(
						def.getName(),
						def.getIdSupplier(),
						storageEngine.createStorage(def.getName(), def.getCodec())
							.addIndexes(def.getIndexes())
							.build()
					);
				}).toMap(v -> v.getName(), v -> v).toImmutable();

				return new LocalSiloImpl(
					storageEngine,
					collections
				);
			});
		}
	}
}
