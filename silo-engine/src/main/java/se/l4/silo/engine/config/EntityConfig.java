package se.l4.silo.engine.config;

import se.l4.exobytes.Expose;
import se.l4.exobytes.Use;
import se.l4.exobytes.internal.reflection.ReflectionSerializer;
import se.l4.silo.Entity;

/**
 * Configuration for an individual {@link Entity}. Every entity type can have
 * their own configuration data, but they must extend this class.
 *
 * @author Andreas Holstenson
 *
 */
@Use(ReflectionSerializer.class)
public class EntityConfig
	extends ConvertableConfig
{
	@Expose
	private String type;

	public EntityConfig()
	{
	}

	public EntityConfig(@Expose("type") String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
	}
}
