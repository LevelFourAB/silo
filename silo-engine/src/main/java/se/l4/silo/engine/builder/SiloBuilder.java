package se.l4.silo.engine.builder;

import com.google.inject.Provides;

import se.l4.silo.Silo;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineFactory;

/**
 * Builder for instances of {@link Silo}. Should be used in a method annotated
 * with {@link Provides}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface SiloBuilder
{
	/**
	 * Register a new {@link QueryEngine}. This can be referenced by its name
	 * for entity configurations.
	 * 
	 * @param name
	 * @param factory
	 * @return
	 */
	SiloBuilder addQueryEngine(String name, QueryEngineFactory factory);
	
	/**
	 * Create this instance.
	 * 
	 * @param configSet
	 * @return
	 */
	Silo build();
}
