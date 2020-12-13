package se.l4.silo.engine;

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
	QueryEngine<?, ?> create(QueryEngineCreationEncounter encounter);
}
