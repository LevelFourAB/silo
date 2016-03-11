package se.l4.silo.engine.builder;

import se.l4.aurochs.serialization.DefaultSerializerCollection;
import se.l4.aurochs.serialization.SerializerCollection;
import se.l4.silo.Silo;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.types.FieldType;

/**
 * Builder for instances of {@link Silo}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface SiloBuilder
{
	/**
	 * Set the serializer collection to use. If this is not called an
	 * instance of {@link DefaultSerializerCollection} will be used.
	 * 
	 * @param collection
	 * @return
	 */
	SiloBuilder withSerializerCollection(SerializerCollection collection);
	
	/**
	 * Add a new entity to this instance.
	 * 
	 * @param name
	 * @return
	 */
	EntityBuilder<SiloBuilder> addEntity(String name);
	
	/**
	 * Register a new {@link QueryEngine}. This can be referenced by its name
	 * for entity configurations.
	 * 
	 * @param name
	 * @param factory
	 * @return
	 */
	SiloBuilder addQueryEngine(QueryEngineFactory<?, ?> factory);
	
	/**
	 * Register a {@link FieldType} that can be used by entities.
	 * 
	 * @param fieldType
	 * @return
	 */
	SiloBuilder addFieldType(FieldType<?> fieldType);
	
	/**
	 * Create this instance.
	 * 
	 * @param configSet
	 * @return
	 */
	Silo build();
}
