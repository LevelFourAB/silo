package se.l4.silo.engine;

import java.nio.file.Path;

import se.l4.silo.engine.config.QueryEngineConfig;

/**
 * Encounter for when a {@link QueryEngine} is being constructed.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryEngineCreationEncounter<Config extends QueryEngineConfig>
{
	/**
	 * Get the name of the engine being created.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Get the configuration of this query engine.
	 * 
	 * @return
	 */
	Config getConfig();
	
	/**
	 * Get access to fields defined on the entity this query engine will
	 * belong to.
	 * 
	 * @return
	 */
	Fields getFields();
	
	/**
	 * Resolve a name against the data directory of the entity the query
	 * engine will belong to.
	 *  
	 * @param name
	 * @return
	 */
	Path resolveDataFile(String name);
	
	/**
	 * Resolve a path against the data directory of the entity the query
	 * engine will belong to.
	 * 
	 * @param path
	 * @return
	 */
	Path resolveDataFile(Path path);
	
	/**
	 * Open a new MVStore with the given name. The name will be passed to
	 * {@link #resolveDataFile(String)} to find where the data should be
	 * stored.
	 * 
	 * @param name
	 * @return
	 */
	MVStoreManager openMVStore(String name); 
}
