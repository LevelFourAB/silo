package se.l4.silo.engine.internal;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.StorageException;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngineFactory;

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
	private final ImmutableMap<String, QueryEngineFactory<?>> queryEngines;

	public LocalEngineFactories(Iterable<EntityTypeFactory<?, ?>> entityTypes, 
			Iterable<QueryEngineFactory<?>> queryTypes)
	{
		ImmutableMap.Builder<String, EntityTypeFactory<?,?>> entities = ImmutableMap.builder();
		for(EntityTypeFactory<?, ?> f : entityTypes)
		{
			entities.put(f.getId(), f);
		}
		this.entities = entities.build();
		
		ImmutableMap.Builder<String, QueryEngineFactory<?>> queryEngines = ImmutableMap.builder();
		for(QueryEngineFactory<?> f : queryTypes)
		{
			queryEngines.put(f.getId(), f);
		}
		this.queryEngines = queryEngines.build();
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

}
