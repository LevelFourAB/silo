package se.l4.silo.engine.internal;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.StorageException;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.types.FieldType;

/**
 * Implementation of {@link EngineFactories} used by {@link LocalSilo}.
 *
 * @author Andreas Holstenson
 *
 */
public class LocalEngineFactories
	implements EngineFactories
{
	private final ImmutableMap<String, EntityTypeFactory<?, ?>> entities;
	private final ImmutableMap<String, QueryEngineFactory<?, ?>> queryEngines;
	private final ImmutableMap<String, FieldType<?>> fieldTypes;

	public LocalEngineFactories(Iterable<EntityTypeFactory<?, ?>> entityTypes,
			Iterable<QueryEngineFactory<?, ?>> queryTypes,
			Iterable<FieldType<?>> fieldTypes)
	{
		ImmutableMap.Builder<String, EntityTypeFactory<?,?>> entities = ImmutableMap.builder();
		for(EntityTypeFactory<?, ?> f : entityTypes)
		{
			entities.put(f.getId(), f);
		}
		this.entities = entities.build();

		ImmutableMap.Builder<String, QueryEngineFactory<?, ?>> queryEngines = ImmutableMap.builder();
		for(QueryEngineFactory<?, ?> f : queryTypes)
		{
			queryEngines.put(f.getId(), f);
		}
		this.queryEngines = queryEngines.build();

		ImmutableMap.Builder<String, FieldType<?>> fieldTypeBuilder = ImmutableMap.builder();
		for(FieldType<?> f : fieldTypes)
		{
			fieldTypeBuilder.put(f.uniqueId(), f);
		}
		this.fieldTypes = fieldTypeBuilder.build();
	}

	@Override
	public EntityTypeFactory<?, ?> forEntity(String type)
	{
		EntityTypeFactory<?, ?> factory = entities.get(type);
		if(factory == null)
		{
			throw new StorageException("Don't know how to construct entities of type " + type);
		}

		return factory;
	}

	@Override
	public QueryEngineFactory<?, ?> forQueryEngine(String type)
	{
		QueryEngineFactory<?, ?> factory = queryEngines.get(type);
		if(factory == null)
		{
			throw new StorageException("Unknown query engine " + type);
		}

		return factory;
	}

	@Override
	public FieldType<?> getFieldType(String type)
	{
		FieldType<?> ft = fieldTypes.get(type);
		if(ft == null)
		{
			throw new StorageException("The field type " + type + " is unknown");
		}

		return ft;
	}
}
