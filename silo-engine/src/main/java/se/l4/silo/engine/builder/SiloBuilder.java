package se.l4.silo.engine.builder;

import se.l4.commons.serialization.DefaultSerializerCollection;
import se.l4.commons.serialization.SerializerCollection;
import se.l4.commons.types.TypeFinder;
import se.l4.silo.Silo;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.types.FieldType;
import se.l4.vibe.Vibe;

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
	 * Set the auto loader to use. If this is set the created instance will
	 * be able to pick up extensions automatically.
	 *
	 * @param loader
	 * @return
	 */
	SiloBuilder withTypeFinder(TypeFinder finder);

	/**
	 * Set the {@link Vibe} instance to use. Setting this will enable reporting
	 * of health values. If no path is specified the metrics will be stored
	 * with the prefix {@code silo}.
	 *
	 * @param vibe
	 * @param path
	 * @return
	 */
	SiloBuilder withVibe(Vibe vibe, String... path);

	/**
	 * Add a new type of entity.
	 *
	 * @param type
	 * @return
	 */
	SiloBuilder addEntityType(EntityTypeFactory<?, ?> type);

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
	 * Set the size used to cache data in this instance. This controls the cache size of the main storage, but does
	 * do anything for indexes.
	 *
	 * @param cacheSizeInMb
	 * @return
	 */
	SiloBuilder withCacheSize(int cacheSizeInMb);

	/**
	 * Create this instance.
	 *
	 * @param configSet
	 * @return
	 */
	LocalSilo build();

}
