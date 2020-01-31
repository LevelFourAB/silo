package se.l4.silo.engine.search.scoring;

public interface QueryScorer
{
	/**
	 * Score the given document.
	 *
	 * @param doc
	 * @return
	 */
	double score(int doc);
}
