package se.l4.silo.engine.internal.builder;

import java.util.function.Function;

import se.l4.silo.engine.QueryEngineBuilderFactory;
import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.builder.FieldDefBuilder;
import se.l4.silo.engine.builder.FieldsHelper;
import se.l4.silo.engine.builder.StructuredEntityBuilder;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.config.StructuredEntityConfig;

public class StructuredEntityBuilderImpl<Parent>
	implements StructuredEntityBuilder<Parent>
{
	private final Function<EntityConfig, Parent> configReceiver;
	private final FieldsHelper<StructuredEntityBuilder<Parent>> fields;
	
	private final StructuredEntityConfig config;

	public StructuredEntityBuilderImpl(Function<EntityConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		config = new StructuredEntityConfig();
		fields = new FieldsHelper<>(this);
	}
	
	@Override
	public Parent done()
	{
		config.fields = this.fields.build();
		return configReceiver.apply(config);
	}
	
	@Override
	public FieldDefBuilder<StructuredEntityBuilder<Parent>> defineField(String field)
	{
		return fields.defineField(field);
	}
	
	@Override
	public StructuredEntityBuilder<Parent> defineField(String field, String type)
	{
		return fields.defineField(field, type);
	}

	@Override
	public <T extends BuilderWithParent<StructuredEntityBuilder<Parent>>> T add(String name,
			QueryEngineBuilderFactory<StructuredEntityBuilder<Parent>, T> factory)
	{
		return factory.create(queryConfig -> {
			config.addQueryEngine(name, queryConfig);
			return this;
		});
	}
}
