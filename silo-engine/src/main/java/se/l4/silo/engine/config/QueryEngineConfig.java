package se.l4.silo.engine.config;

import se.l4.exobytes.Expose;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;

/**
 * Abstract configuration for a {@link QueryEngine}.
 *
 * @author Andreas Holstenson
 *
 */
public abstract class QueryEngineConfig
	extends ConvertableConfig
{
	/**
	 * The type of this config, should match {@link QueryEngineFactory#getId()}.
	 */
	@Expose
	private String type;

	public QueryEngineConfig()
	{
	}

	public QueryEngineConfig(String type)
	{
		this.type = type;
	}

	/**
	 * Get the type that this query engine should be.
	 *
	 * @return
	 */
	public String getType()
	{
		return type;
	}
}
