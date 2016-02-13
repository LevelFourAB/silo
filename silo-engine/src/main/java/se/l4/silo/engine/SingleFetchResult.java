package se.l4.silo.engine;

import java.util.Iterator;

import com.google.common.collect.Iterators;

import se.l4.silo.FetchResult;

public class SingleFetchResult<T>
	implements FetchResult<T>
{
	private final T item;

	public SingleFetchResult(T item)
	{
		this.item = item;
	}

	@Override
	public Iterator<T> iterator()
	{
		return Iterators.singletonIterator(item);
	}

	@Override
	public int getSize()
	{
		return 1;
	}

	@Override
	public int getOffset()
	{
		return 0;
	}

	@Override
	public int getLimit()
	{
		return 0;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public void close()
	{

	}
	
	@Override
	public String toString()
	{
		return "SingleFetchResult{" + item + "}";
	}
}
