package se.l4.silo.engine.internal;

import se.l4.exobytes.Serializers;
import se.l4.silo.engine.EntityCreationEncounter;
import se.l4.silo.engine.builder.StorageBuilder;

/**
 * Implementation of {@link EntityCreationEncounter}.
 *
 * @author Andreas Holstenson
 *
 * @param <Config>
 */
public class EntityCreationEncounterImpl<Config>
	implements EntityCreationEncounter<Config>
{
	private final StorageEngine engine;
	private final String name;
	private final Config config;
	private final Serializers serializers;

	public EntityCreationEncounterImpl(Serializers serializers, StorageEngine engine, String name, Config config)
	{
		this.serializers = serializers;
		this.engine = engine;
		this.name = name;
		this.config = config;
	}

	@Override
	public String getEntityName()
	{
		return name;
	}

	@Override
	public Config getConfig()
	{
		return config;
	}

	@Override
	public Serializers getSerializerCollection()
	{
		return serializers;
	}

	@Override
	public StorageBuilder createMainEntity()
	{
		return engine.createStorage(name);
	}

	@Override
	public StorageBuilder createSubEntity(String sub)
	{
		return engine.createStorage(name, null);
	}
}
