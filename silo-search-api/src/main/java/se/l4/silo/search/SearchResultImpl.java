package se.l4.silo.search;

import java.util.Iterator;

import se.l4.silo.FetchResult;

/**
 * Implementation of {@link SearchResult}.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class SearchResultImpl<T>
	implements SearchResult<T>
{
	private final FetchResult<SearchHit<T>> results;
	private final Facets facets;

	public SearchResultImpl(FetchResult<SearchHit<T>> results, Facets facets)
	{
		this.results = results;
		this.facets = facets;
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
	public Facets facets()
	{
		return facets;
	}

	@Override
	public void close()
	{
		results.close();
	}
	
	
}