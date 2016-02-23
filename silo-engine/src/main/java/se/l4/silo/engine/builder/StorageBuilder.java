package se.l4.silo.engine.builder;

import se.l4.silo.Entity;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.config.QueryableEntityConfig;

/**
 * Builder for instances of {@link Storage} that are used by different
 * {@link Entity entity types} to store data.
 * 
 * @author Andreas Holstenson
 *
 */
public interface StorageBuilder
{
	/**
	 * Add all query engines from the given configuration.
	 * 
	 * @param config
	 * @return
	 */
	StorageBuilder withQueryEngines(QueryableEntityConfig config);
	
	/**
	 * Add a specific query engine to this storage.
	 * 
	 * @param factory
	 * @param config
	 * @return
	 */
	<C> StorageBuilder withQueryEngine(QueryEngineFactory<?> factory, C config);
	
	/**
	 * Build and return the storage.
	 * 
	 * @return
	 */
	Storage build();
}
