package se.l4.silo.engine.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import se.l4.silo.Silo;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.IndexQueryEngineFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.builder.EntityBuilder;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.internal.binary.BinaryEntityFactory;
import se.l4.silo.engine.internal.builder.EntityBuilderImpl;
import se.l4.silo.engine.internal.structured.StructuredEntityFactory;
import se.l4.silo.engine.log.LogBuilder;
import se.l4.silo.engine.types.BooleanFieldType;
import se.l4.silo.engine.types.ByteArrayFieldType;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.IntFieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;

public class LocalSiloBuilder
	implements SiloBuilder
{
	private final LogBuilder logBuilder;
	private final Path dataPath;
	
	private final List<EntityTypeFactory<?, ?>> entityTypes;
	private final List<QueryEngineFactory<?, ?>> queryEngineTypes;
	private final List<FieldType<?>> fieldTypes;
	
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
		queryEngineTypes.add(IndexQueryEngineFactory.type());
		
		fieldTypes = new ArrayList<>();
		fieldTypes.add(BooleanFieldType.INSTANCE);
		fieldTypes.add(ByteArrayFieldType.INSTANCE);
		fieldTypes.add(IntFieldType.INSTANCE);
		fieldTypes.add(LongFieldType.INSTANCE);
		fieldTypes.add(StringFieldType.INSTANCE);
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
	public SiloBuilder addFieldType(FieldType<?> fieldType)
	{
		fieldTypes.add(fieldType);
		return this;
	}
	
	@Override
	public Silo build()
	{
		LocalEngineFactories factories = new LocalEngineFactories(entityTypes, queryEngineTypes, fieldTypes);
		return new LocalSilo(factories, logBuilder, dataPath, config);
	}
}
