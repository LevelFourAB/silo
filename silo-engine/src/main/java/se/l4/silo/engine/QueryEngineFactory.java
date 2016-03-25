package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.config.QueryEngineConfig;

/**
 * Factory for configuring and building instances of {@link QueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Builder>
 */
public interface QueryEngineFactory<Builder extends BuilderWithParent<?>, Config extends QueryEngineConfig>
{
	/**
	 * Get the identifier of this query engine.
	 * 
	 * @return
	 */
	String getId();
	
	/**
	 * Create a builder that builds up the internal configuration of an
	 * instance of this query engine.
	 * 
	 * @param parent
	 * @return
	 */
	<T> Builder builder(Function<QueryEngineConfig, T> configReceiver);
	
	/**
	 * Get the type of configuration this factory expects.
	 * 
	 * @return
	 */
	Class<Config> getConfigClass();
	
	/**
	 * Create a new instance of this query engine from the given configuration.
	 * 
	 * @param config
	 * @return
	 */
	QueryEngine<?> create(QueryEngineCreationEncounter<Config> encounter);
}
