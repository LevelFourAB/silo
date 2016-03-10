package se.l4.silo.engine.config;

import java.util.List;

import se.l4.aurochs.serialization.Expose;
import se.l4.aurochs.serialization.ReflectionSerializer;
import se.l4.aurochs.serialization.Use;

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
