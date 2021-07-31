package se.l4.silo.index.search.internal;

import java.util.Objects;

import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.query.BoostQuery;

/**
 * Implementation of {@link BoostQuery}.
 */
public class BoostQueryImpl
	implements BoostQuery
{
	private final QueryClause clause;
	private final float boost;

	public BoostQueryImpl(
		QueryClause clause,
		float boost
	)
	{
		this.clause = clause;
		this.boost = boost;
	}

	@Override
	public QueryClause getClause()
	{
		return clause;
	}

	@Override
	public float getBoost()
	{
		return boost;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(boost, clause);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		BoostQueryImpl other = (BoostQueryImpl) obj;
		return Float.floatToIntBits(boost) == Float.floatToIntBits(other.boost)
			&& Objects.equals(clause, other.clause);
	}

	@Override
	public String toString()
	{
		return "BoostQuery{boost=" + boost + ", clause=" + clause + "}";
	}
}
