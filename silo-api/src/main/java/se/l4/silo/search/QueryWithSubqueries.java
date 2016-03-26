package se.l4.silo.search;

import se.l4.silo.search.query.AndQuery;
import se.l4.silo.search.query.ConstantScoreQuery;
import se.l4.silo.search.query.FieldQuery;
import se.l4.silo.search.query.NegateQuery;
import se.l4.silo.search.query.OrQuery;
import se.l4.silo.search.query.QueryReceiver;
import se.l4.silo.search.query.RangeQuery;
import se.l4.silo.search.query.SuggestQuery;
import se.l4.silo.search.query.UserQuery;

public interface QueryWithSubqueries<Self extends QueryWithSubqueries<Self, ReturnPath>, ReturnPath>
	extends QueryPart<ReturnPath>, QueryReceiver
{
	<P extends QueryPart<Self>> P query(P q);
	
	ReturnPath done();
	
	default Self user(String text)
	{
		return query(new UserQuery<>()).text(text);
	}
	
	default OrQuery<Self> or()
	{
		return query(new OrQuery<>());
	}
	
	default AndQuery<Self> and()
	{
		return query(new AndQuery<>());
	}
	
	default NegateQuery<Self> negate()
	{
		return query(new NegateQuery<>());
	}
	
	default FieldQuery<Self> field(String field)
	{
		return query(new FieldQuery<>(field));
	}
	
	default ConstantScoreQuery<Self> constantScore(float score)
	{
		return query(new ConstantScoreQuery<>(score));
	}
	
	default SuggestQuery<Self> suggest(String field)
	{
		return query(new SuggestQuery<>(field));
	}
	
	default RangeQuery<Self> number(String field)
	{
		return query(new RangeQuery<>(field));
	}
}
