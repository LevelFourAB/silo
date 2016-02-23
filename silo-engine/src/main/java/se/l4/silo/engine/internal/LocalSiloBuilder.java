package se.l4.silo.engine.internal;

import java.nio.file.Path;
import java.util.ArrayList;

import se.l4.silo.Silo;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.IndexQueryEngine;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.builder.EntityBuilder;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.internal.binary.BinaryEntityFactory;
import se.l4.silo.engine.internal.builder.EntityBuilderImpl;
import se.l4.silo.engine.internal.structured.StructuredEntityFactory;
import se.l4.silo.engine.log.LogBuilder;

public class LocalSiloBuilder
	implements SiloBuilder
{
	private final LogBuilder logBuilder;
	private final Path dataPath;
	
	private final ArrayList<EntityTypeFactory<?, ?>> entityTypes;
	private final ArrayList<QueryEngineFactory<?>> queryEngineTypes;
	
	private EngineConfig config;

	public LocalSiloBuilder(LogBuilder logBuilder, Path dataPath)
	{
		this.logBuilder = logBuilder;
		this.dataPath = dataPath;
		
		config = new EngineConfig();
		
		entityTypes = new ArrayList<>();
		entityTypes.add(new BinaryEntityFactory());
		entityTypes.add(new StructuredEntityFactory());
		
		queryEngineTypes = new ArrayList<>();
		queryEngineTypes.add(IndexQueryEngine.type());
	}
	
	@Override
	public EntityBuilder<SiloBuilder> addEntity(String name)
	{
		return new EntityBuilderImpl<>(c -> {
			config = config.addEntity(name, c);
			return this;
		});
	}
	
	@Override
	public SiloBuilder addQueryEngine(QueryEngineFactory factory)
	{
		queryEngineTypes.add(factory);
		return this;
	}
	
	@Override
	public Silo build()
	{
		LocalEngineFactories factories = new LocalEngineFactories(entityTypes, queryEngineTypes);
		return new LocalSilo(factories, logBuilder, dataPath, config);
	}
}
