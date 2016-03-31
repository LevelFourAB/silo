package se.l4.silo.results;

import java.util.Collections;
import java.util.Iterator;

import se.l4.silo.FetchResult;

/**
 * A {@link FetchResult} that is always empty.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class EmptyFetchResult<T>
	implements FetchResult<T>
{
	public static final EmptyFetchResult<?> INSTANCE = new EmptyFetchResult<>();
	
	private EmptyFetchResult()
	{
	}

	@Override
	public Iterator<T> iterator()
	{
		return Collections.emptyIterator();
	}

	@Override
	public int getSize()
	{
		return 0;
	}

	@Override
	public int getOffset()
	{
		return 0;
	}

	@Override
	public int getLimit()
	{
		return -1;
	}
	
	@Override
	public int getTotal()
	{
		return 0;
	}

	@Override
	public boolean isEmpty()
	{
		return true;
	}

	@Override
	public void close()
	{
	}
	
	@Override
	public String toString()
	{
		return "EmptyFetchResult{}";
	}
}
