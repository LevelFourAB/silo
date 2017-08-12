package se.l4.silo.search;

import se.l4.silo.search.query.QueryReceiver;

public interface QueryWithSubqueries<Self extends QueryWithSubqueries<Self, ReturnPath>, ReturnPath>
	extends QueryCriteriaBuilder<Self>, QueryPart<ReturnPath>, QueryReceiver
{
	
	ReturnPath done();
	
}
