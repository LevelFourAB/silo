package se.l4.silo.engine.config;

import java.util.HashMap;
import java.util.Map;

import se.l4.silo.Entity;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.builder.SiloBuilder;

/**
 * Configuration for an engine consisting of several {@link Entity entities}.
 * Instances of this class can be created via a {@link SiloBuilder} retrieved
 * from for example {@link LocalSilo}.
 * 
 * @author Andreas Holstenson
 *
 */
public class EngineConfig
{
	private final Map<String, EntityConfig> entities;
	private final int cacheSizeInMb;
	
	public EngineConfig()
	{
		this(new HashMap<>(), 128);
	}
	
	public EngineConfig(Map<String, EntityConfig> entities, int cacheSizeInMb)
	{
		this.entities = entities;
		this.cacheSizeInMb = cacheSizeInMb;
	}
	
	/**
	 * Get all of the entities configured for this engine.
	 * 
	 * @return
	 */
	public Map<String, EntityConfig> getEntities()
	{
		return entities;
	}
	
	/**
	 * Add a an entity to this configuration and return a new instance of the
	 * configuration.
	 * 
	 * @param entity
	 * @param config
	 * @return
	 */
	public EngineConfig addEntity(String entity, EntityConfig config)
	{
		HashMap<String, EntityConfig> newEntities = new HashMap<>(entities);
		newEntities.put(entity, config);
		return new EngineConfig(newEntities, cacheSizeInMb);
	}
	
	public int getCacheSizeInMb()
	{
		return cacheSizeInMb;
	}
	
	public EngineConfig setCacheSizeInMb(int cacheSizeInMb)
	{
		return new EngineConfig(entities, cacheSizeInMb);
	}
}
