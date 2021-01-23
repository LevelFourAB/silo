package se.l4.silo.engine.index;

public interface IndexDefinition<T>
{
	/**
	 * Get the name of the index.
	 */
	String getName();

	/**
	 * Create a new instance of this query engine from the given configuration.
	 *
	 * @param config
	 * @return
	 */
	Index<?, ?> create(IndexEngineCreationEncounter encounter);
}
