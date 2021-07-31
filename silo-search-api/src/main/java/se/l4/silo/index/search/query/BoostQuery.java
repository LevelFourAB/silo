package se.l4.silo.index.search.query;

import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.internal.BoostQueryImpl;

/**
 * {@link QueryClause} to increase or lower the important of another clause.
 */
public interface BoostQuery
	extends QueryClause
{
	/**
	 * The clause to apply a boost to.
	 *
	 * @return
	 */
	QueryClause getClause();

	/**
	 * Boost to apply. Less than one will reduce the importance of the clause,
	 * more than one will increase the important of the clause.
	 *
	 * @return
	 */
	float getBoost();

	/**
	 * Create an instance that will boost the given clause.
	 *
	 * @param clause
	 *   clause that will be boosted
	 * @param boost
	 *   boost to apply. Less than one will reduce the importance of the clause,
	 *   more than one will increase the important of the clause.
	 * @return
	 */
	static BoostQuery create(QueryClause clause, float boost)
	{
		return new BoostQueryImpl(clause, boost);
	}
}

