package se.l4.silo.engine.config;

import se.l4.commons.serialization.Expose;
import se.l4.commons.serialization.ReflectionSerializer;
import se.l4.commons.serialization.Use;
import se.l4.silo.engine.FieldDef;

/**
 * Basic implementation of {@link FieldDef}.
 *
 * @author Andreas Holstenson
 *
 */
@Use(ReflectionSerializer.class)
public class FieldConfig
{
	@Expose
	private final String name;
	@Expose
	private final String type;
	@Expose
	private final boolean collection;

	public FieldConfig(@Expose("name") String name, @Expose("type") String type, @Expose("collection") boolean collection)
	{
		this.name = name;
		this.type = type;
		this.collection = collection;
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public boolean isCollection()
	{
		return collection;
	}
}
