package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;

/**
 * Factory for configuring and building instances of {@link QueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Builder>
 */
public interface QueryEngineFactory<Builder extends BuilderWithParent<?>>
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
	<T> Builder builder(Function<Object, T> configReceiver);
}
