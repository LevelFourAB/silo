package se.l4.silo.engine.internal;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import se.l4.commons.config.ConfigException;
import se.l4.commons.serialization.DefaultSerializerCollection;
import se.l4.commons.serialization.SerializerCollection;
import se.l4.commons.types.TypeFinder;
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
	
	private final Map<String, EntityTypeFactory<?, ?>> entityTypes;
	private final Map<String, QueryEngineFactory<?, ?>> queryEngineTypes;
	private final Map<String, FieldType<?>> fieldTypes;
	
	private EngineConfig config;
	private SerializerCollection serializers;
	private TypeFinder typeFinder;

	public LocalSiloBuilder(LogBuilder logBuilder, Path dataPath)
	{
		this.logBuilder = logBuilder;
		this.dataPath = dataPath;
		
		config = new EngineConfig();
		
		entityTypes = new HashMap<>();
		queryEngineTypes = new HashMap<>();
		fieldTypes = new HashMap<>();
		
		addEntityType(new BinaryEntityFactory());
		addEntityType(new StructuredEntityFactory());
		
		addQueryEngine(IndexQueryEngineFactory.type());
		
		addFieldType(BooleanFieldType.INSTANCE);
		addFieldType(ByteArrayFieldType.INSTANCE);
		addFieldType(IntFieldType.INSTANCE);
		addFieldType(LongFieldType.INSTANCE);
		addFieldType(StringFieldType.INSTANCE);
	}
	
	private <T> void put(Map<String, T> map, T instance, String name)
	{
		if(map.containsKey(name))
		{
			throw new ConfigException("Can not register " + instance + " with id " + name);
		}
		
		map.put(name, instance);
	}
	
	@Override
	public SiloBuilder withSerializerCollection(SerializerCollection collection)
	{
		this.serializers = collection;
		return this;
	}
	
	@Override
	public SiloBuilder withTypeFinder(TypeFinder typeFinder)
	{
		this.typeFinder = typeFinder;
		return this;
	}
	
	@Override
	public EntityBuilder<SiloBuilder> addEntity(String name)
	{
		return new EntityBuilderImpl<>(c -> {
			config = config.addEntity(name, c);
			return this;
		});
	}
	
	public SiloBuilder addEntityType(EntityTypeFactory<?, ?> type)
	{
		put(entityTypes, type, type.getId());
		return this;
	}
	
	@Override
	public SiloBuilder addQueryEngine(QueryEngineFactory factory)
	{
		put(queryEngineTypes, factory, factory.getId());
		return this;
	}
	
	@Override
	public SiloBuilder addFieldType(FieldType<?> fieldType)
	{
		put(fieldTypes, fieldType, fieldType.uniqueId());
		return this;
	}
	
	@Override
	public LocalSilo build()
	{
		autoLoad();
		
		LocalEngineFactories factories = new LocalEngineFactories(entityTypes.values(), queryEngineTypes.values(), fieldTypes.values());
		SerializerCollection serializers = this.serializers == null ? new DefaultSerializerCollection() : this.serializers;
		return new LocalSilo(factories, serializers, logBuilder, dataPath, config);
	}

	private void autoLoad()
	{
		if(typeFinder == null) return;
		
		// Locate any query engine types available
		for(QueryEngineFactory<?, ?> qe : typeFinder.getSubTypesAsInstances(QueryEngineFactory.class))
		{
			addQueryEngine(qe);
		}
		
		// Find all available field types
		for(FieldType<?> ft : typeFinder.getSubTypesAsInstances(FieldType.class))
		{
			addFieldType(ft);
		}
		
		// Find all entity types
		for(EntityTypeFactory<?, ?> et : typeFinder.getSubTypesAsInstances(EntityTypeFactory.class))
		{
			addEntityType(et);
		}
	}
}
