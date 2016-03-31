package se.l4.silo.search;

import se.l4.silo.search.query.QueryReceiver;

public interface QueryPart<ReturnPath>
{
	void parent(ReturnPath path, QueryReceiver receiver);
}
