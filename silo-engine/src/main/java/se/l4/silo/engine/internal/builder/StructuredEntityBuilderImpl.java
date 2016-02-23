package se.l4.silo.engine.internal.builder;

import java.util.function.Function;

import se.l4.silo.engine.IndexQueryEngine;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.builder.IndexBuilder;
import se.l4.silo.engine.builder.StructuredEntityBuilder;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.config.StructuredEntityConfig;

public class StructuredEntityBuilderImpl<Parent>
	implements StructuredEntityBuilder<Parent>
{
	private Function<EntityConfig, Parent> configReceiver;
	private StructuredEntityConfig config;

	public StructuredEntityBuilderImpl(Function<EntityConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		config = new StructuredEntityConfig();
	}

	@Override
	public Parent done()
	{
		return configReceiver.apply(config);
	}

	@Override
	public IndexBuilder<StructuredEntityBuilder<Parent>> addIndex(String name)
	{
		return add(name, IndexQueryEngine.type());
	}

	@Override
	public <T extends BuilderWithParent<StructuredEntityBuilder<Parent>>> T add(String name, QueryEngineFactory<T> type)
	{
		return type.builder(queryConfig -> {
			config.addQueryEngine(name, queryConfig);
			return this;
		});
	}

}
