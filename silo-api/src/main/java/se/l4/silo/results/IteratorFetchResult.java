package se.l4.silo.results;

import java.util.Collection;
import java.util.Iterator;

import se.l4.silo.FetchResult;

/**
 * {@link FetchResult} implemented over an {@link Iterator}.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class IteratorFetchResult<T>
	implements FetchResult<T>
{
	private Iterator<T> it;
	private int size;
	private int offset;
	private int limit;
	private int total;

	public IteratorFetchResult(Iterator<T> it, int size, int offset, int limit, int total)
	{
		this.it = it;
		this.size = size;
		this.offset = offset;
		this.limit = limit;
		this.total = total;
	}
	
	public IteratorFetchResult(Collection<T> data, int offset, int limit, int total)
	{
		this(data.iterator(), data.size(), offset, limit, total);
	}

	@Override
	public Iterator<T> iterator()
	{
		return it;
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public int getOffset()
	{
		return offset;
	}

	@Override
	public int getLimit()
	{
		return limit;
	}
	
	@Override
	public int getTotal()
	{
		return total;
	}

	@Override
	public boolean isEmpty()
	{
		return size == 0;
	}

	@Override
	public void close()
	{
		
	}
}
