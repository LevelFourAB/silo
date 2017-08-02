package se.l4.silo.engine.config;

import java.util.HashMap;
import java.util.Map;

import se.l4.silo.Entity;
import se.l4.silo.engine.QueryEngine;

/**
 * Abstract base for {@link Entity entities} that support
 * {@link QueryEngine query engines}.
 *
 * @author Andreas Holstenson
 *
 */
public abstract class QueryableEntityConfig
	extends EntityConfig
{
	public Map<String, QueryEngineConfig> queryEngines;

	public QueryableEntityConfig()
	{
		this(null);
	}

	public QueryableEntityConfig(String type)
	{
		super(type);

		queryEngines = new HashMap<>();
	}

	public void addQueryEngine(String name, QueryEngineConfig config)
	{
		queryEngines.put(name, config);
	}

	public Map<String, QueryEngineConfig> getQueryEngines()
	{
		return queryEngines;
	}
}
