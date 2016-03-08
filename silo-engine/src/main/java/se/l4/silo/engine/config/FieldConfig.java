package se.l4.silo.engine.config;

import se.l4.silo.engine.FieldDef;

/**
 * Basic implementation of {@link FieldDef}.
 * 
 * @author Andreas Holstenson
 *
 */
public class FieldConfig
{
	private final String name;
	private final String type;
	private final boolean collection;

	public FieldConfig(String name, String type, boolean collection)
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
