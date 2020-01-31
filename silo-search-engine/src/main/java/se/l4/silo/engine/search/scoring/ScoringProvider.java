package se.l4.silo.engine.search.scoring;

import java.io.IOException;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.IndexDefinitionEncounter;

public interface ScoringProvider<T>
{
	/**
	 * Get the id of this provider.
	 *
	 * @return
	 */
	String id();

	/**
	 * Enhance the current {@link IndexDefinition} with extra information needed
	 * for this scoring to function.
	 *
	 * @param encounter
	 */
	default void setup(IndexDefinitionEncounter encounter)
	{
	}

	/**
	 * If this scoring needs the scores calculated by the query or if it works
	 * without them.
	 *
	 * @return
	 */
	default boolean needsScores()
	{
		return false;
	}

	/**
	 * Create a scorer for the given encounter.
	 *
	 * @param encounter
	 * @return
	 * @throws IOException
	 */
	QueryScorer createScorer(ScoringEncounter<T> encounter)
		throws IOException;
}
