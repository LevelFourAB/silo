package se.l4.silo.search;

import se.l4.silo.search.query.ConstantScoreQuery;
import se.l4.silo.search.query.FieldQuery;
import se.l4.silo.search.query.NegateQuery;
import se.l4.silo.search.query.OrQuery;
import se.l4.silo.search.query.QueryReceiver;
import se.l4.silo.search.query.RangeQuery;
import se.l4.silo.search.query.SuggestQuery;
import se.l4.silo.search.query.UserQuery;

public interface QueryWithSubquery<Self extends QueryWithSubquery<Self, ReturnPath>, ReturnPath>
	extends QueryPart<ReturnPath>, QueryReceiver
{
	<P extends QueryPart<ReturnPath>> P query(P q);
	
	default UserQuery<ReturnPath> user(String... fields)
	{
		return query(new UserQuery<>(fields));
	}
	
	default OrQuery<ReturnPath> or()
	{
		return query(new OrQuery<>());
	}
	
	default NegateQuery<ReturnPath> negate()
	{
		return query(new NegateQuery<>());
	}
	
	default FieldQuery<ReturnPath> field(String field)
	{
		return query(new FieldQuery<>(field));
	}
	
	default ConstantScoreQuery<ReturnPath> constantScore(float score)
	{
		return query(new ConstantScoreQuery<>(score));
	}
	
	default SuggestQuery<ReturnPath> suggest(String field)
	{
		return query(new SuggestQuery<>(field));
	}
	
	default RangeQuery<ReturnPath> number(String field)
	{
		return query(new RangeQuery<>(field));
	}
}
