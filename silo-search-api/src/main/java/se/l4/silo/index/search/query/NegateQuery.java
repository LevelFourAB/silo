package se.l4.silo.index.search.query;

import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.internal.NegateQueryImpl;

/**
 * {@link QueryClause} used to negate another clause.
 */
public interface NegateQuery
	extends QueryClause
{
	/**
	 * The clause that may no longer match.
	 *
	 * @return
	 */
	QueryClause getClause();

	static NegateQuery create(QueryClause other)
	{
		return new NegateQueryImpl(other);
	}
}
