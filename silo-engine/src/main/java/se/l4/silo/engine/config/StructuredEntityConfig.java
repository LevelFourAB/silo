package se.l4.silo.engine.config;

import java.util.List;

import se.l4.exobytes.Expose;
import se.l4.exobytes.Use;
import se.l4.exobytes.internal.reflection.ReflectionSerializer;

@Use(ReflectionSerializer.class)
public class StructuredEntityConfig
	extends QueryableEntityConfig
{
	@Expose
	public List<FieldConfig> fields;

	public StructuredEntityConfig()
	{
		super("silo:structured");
	}
}
