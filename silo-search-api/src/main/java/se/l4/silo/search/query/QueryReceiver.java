package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public interface QueryReceiver
{
	void addQuery(QueryItem item);
}
