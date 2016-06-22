package se.l4.silo.engine.search.scoring;

public interface QueryScorer
{
	/**
	 * Score the given document.
	 * 
	 * @param doc
	 * @param currentScore
	 * @return
	 */
	float score(int doc, float currentScore);
}
