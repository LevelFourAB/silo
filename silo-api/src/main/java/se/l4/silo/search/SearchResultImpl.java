package se.l4.silo.search;

import java.util.Iterator;

import se.l4.silo.FetchResult;

public class SearchResultImpl<T>
	implements SearchResult<T>
{
	private FetchResult<SearchHit<T>> results;

	public SearchResultImpl(FetchResult<SearchHit<T>> results)
	{
		this.results = results;
	}

	@Override
	public int getSize()
	{
		return results.getSize();
	}

	@Override
	public int getOffset()
	{
		return results.getOffset();
	}

	@Override
	public int getLimit()
	{
		return results.getLimit();
	}

	@Override
	public Iterator<SearchHit<T>> iterator()
	{
		return results.iterator();
	}

	@Override
	public int getTotal()
	{
		return results.getTotal();
	}

	@Override
	public boolean isEmpty()
	{
		return results.isEmpty();
	}

	@Override
	public void close()
	{
		results.close();
	}
	
	
}
