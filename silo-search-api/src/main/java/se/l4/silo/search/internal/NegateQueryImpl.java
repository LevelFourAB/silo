package se.l4.silo.search.internal;

import java.util.Objects;

import se.l4.silo.search.QueryClause;
import se.l4.silo.search.query.NegateQuery;

/**
 * Implementation of {@link NegateQuery}.
 */
public class NegateQueryImpl
	implements NegateQuery
{
	private final QueryClause clause;

	public NegateQueryImpl(
		QueryClause clause
	)
	{
		this.clause = clause;
	}

	@Override
	public QueryClause getClause()
	{
		return clause;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clause);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		NegateQueryImpl other = (NegateQueryImpl) obj;
		return Objects.equals(clause, other.clause);
	}

	@Override
	public String toString()
	{
		return "NegateQuery{clause=" + clause + "}";
	}
}
