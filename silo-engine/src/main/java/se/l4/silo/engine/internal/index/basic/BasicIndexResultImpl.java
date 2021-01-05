package se.l4.silo.engine.internal.index.basic;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.index.basic.BasicIndexResult;
import se.l4.silo.results.IterableFetchResult;

public class BasicIndexResultImpl<T>
	extends IterableFetchResult<T>
	implements BasicIndexResult<T>
{
	private final long offset;
	private final long limit;
	private final long size;
	private final long total;

	public BasicIndexResultImpl(
		ListIterable<T> iterable,
		long total,
		long offset,
		long limit
	)
	{
		super(iterable);

		this.size = iterable.size();
		this.total = total;
		this.offset = offset;
		this.limit = limit;
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
	public long getSize()
	{
		return size;
	}

	@Override
	public long getTotal()
	{
		return total;
	}
}
