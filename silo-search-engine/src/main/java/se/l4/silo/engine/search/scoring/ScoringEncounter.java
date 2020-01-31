package se.l4.silo.engine.search.scoring;

import java.util.Optional;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;

import se.l4.silo.engine.search.IndexDefinition;

/**
 * Encounter used when a {@link QueryScorer} should be created.
 *
 * @author Andreas Holstenson
 *
 */
public interface ScoringEncounter<T>
{
	/**
	 * Get how the index has been defined.
	 *
	 * @return
	 */
	IndexDefinition getIndexDefinition();

	/**
	 * Get parameters.
	 *
	 * @return
	 */
	T getParameters();

	/**
	 * Get the context to use for loading documents based on their
	 * identifier.
	 *
	 * @return
	 */
	LeafReaderContext getLeafReader();

	/**
	 * Get the calculated scores if they are available.
	 *
	 * @return
	 */
	Optional<DoubleValues> getScores();
}
