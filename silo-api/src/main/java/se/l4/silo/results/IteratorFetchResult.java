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
	private long size;
	private long offset;
	private long limit;
	private long total;

	public IteratorFetchResult(Iterator<T> it, long size, long offset, long limit, long total)
	{
		this.it = it;
		this.size = size;
		this.offset = offset;
		this.limit = limit;
		this.total = total;
	}

	public IteratorFetchResult(Collection<T> data, long offset, long limit, long total)
	{
		this(data.iterator(), data.size(), offset, limit, total);
	}

	@Override
	public Iterator<T> iterator()
	{
		return it;
	}

	@Override
	public long getSize()
	{
		return size;
	}

	@Override
	public long getOffset()
	{
		return offset;
	}

	@Override
	public long getLimit()
	{
		return limit;
	}

	@Override
	public long getTotal()
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
