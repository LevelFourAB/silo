package se.l4.silo.engine.builder;

import se.l4.silo.engine.Index;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.QueryEngineBuilderFactory;
import se.l4.silo.engine.QueryEngineFactory;

/**
 * Interface that indicates that a builder can support {@link QueryEngine}s.
 * Use {@link #add(String, QueryEngineBuilderFactory)} to start defining
 * an engine.
 *
 * @author Andreas Holstenson
 *
 * @param <Self>
 */
public interface HasQueryEngines<Self>
{
	/**
	 * Start adding a new {@link QueryEngine query engine}. The factory is
	 * responsible for creating a configuration that can be used to create
	 * the engine via its {@link QueryEngineFactory} later on.
	 *
	 * <p>
	 * <strong>Important:</strong> Query engines can only be registered if
	 * their {@link QueryEngineFactory} has been previously registered.
	 *
	 * <p>
	 * Most query engines will have a static method called {@code queryEngine} in
	 * their {@link QueryEngineFactory} that can be used as a {@link QueryEngineBuilderFactory}.
	 * Example:
	 *
	 * <pre>
	 * builder.add("testIndex", Index::queryEngine)
	 * </pre>
	 *
	 * @param name
	 * @param factory
	 * @return
	 * @see Index#newIndex
	 */
	<T extends BuilderWithParent<Self>> T add(String name, QueryEngineBuilderFactory<Self, T> factory);
}
