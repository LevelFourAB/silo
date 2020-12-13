package se.l4.silo.engine.internal;

import java.nio.file.Path;

import org.eclipse.collections.api.list.ImmutableList;

import reactor.core.publisher.Mono;
import se.l4.silo.engine.EngineConfig;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.LocalSilo.Builder;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.vibe.Vibe;

public class LocalSiloBuilder
	implements LocalSilo.Builder
{
	private final LogBuilder logBuilder;
	private final Path dataPath;

	private final ImmutableList<EntityDefinition> entities;

	private final EngineConfig config;
	private final Vibe vibe;

	public LocalSiloBuilder(
		LogBuilder logBuilder,
		Path dataPath,

		EngineConfig config,
		ImmutableList<EntityDefinition> entities,

		Vibe vibe
	)
	{
		this.logBuilder = logBuilder;
		this.dataPath = dataPath;
		this.config = config;
		this.vibe = vibe;
		this.entities = entities;
	}

	@Override
	public LocalSilo.Builder withVibe(Vibe vibe, String... path)
	{
		return new LocalSiloBuilder(
			logBuilder,
			dataPath,
			config,
			entities,
			vibe.scope(path)
		);
	}

	@Override
	public Builder addEntity(EntityDefinition<?, ?> definition)
	{
		return new LocalSiloBuilder(
			logBuilder,
			dataPath,
			config,
			entities.newWith(definition),
			vibe
		);
	}

	@Override
	public Builder addEntities(
		Iterable<? extends EntityDefinition<?, ?>> definitions
	)
	{
		return new LocalSiloBuilder(
			logBuilder,
			dataPath,
			config,
			entities.newWithAll(definitions),
			vibe
		);
	}

	@Override
	public Builder withCacheSize(int cacheSizeInMb)
	{
		return new LocalSiloBuilder(
			logBuilder,
			dataPath,
			config.setCacheSizeInMb(cacheSizeInMb),
			entities,
			vibe
		);
	}

	@Override
	public Mono<LocalSilo> start()
	{
		return Mono.fromSupplier(() -> {
			return new LocalSiloImpl(vibe, logBuilder, dataPath, config, entities);
		});
	}
}
