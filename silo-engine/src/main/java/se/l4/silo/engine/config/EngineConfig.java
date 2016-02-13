package se.l4.silo.engine.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for an engine consisting of several entitiese.
 * 
 * @author Andreas Holstenson
 *
 */
public class EngineConfig
{
	private final Map<String, EntityConfig> entities;
	
	public EngineConfig()
	{
		this(new HashMap<>());
	}
	
	public EngineConfig(Map<String, EntityConfig> entities)
	{
		this.entities = entities;
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
		return new EngineConfig(newEntities);
	}
}
