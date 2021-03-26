package se.l4.silo.engine.index;

/**
 * Definition for creating an index in a {@link se.l4.silo.Collection}.
 */
public interface IndexDef<T>
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
