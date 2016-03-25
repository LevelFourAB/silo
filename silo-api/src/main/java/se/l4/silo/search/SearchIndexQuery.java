package se.l4.silo.search;

import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;

public interface SearchIndexQuery<T>
	extends Query<SearchResult<T>>
{
	static <T> QueryType<T, SearchHit<T>, SearchIndexQuery<T>> type()
	{
		return new SearchIndexQueryType<>();
	}
}
