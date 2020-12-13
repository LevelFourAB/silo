package se.l4.silo.engine.internal.index;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.index.FieldIndexResult;
import se.l4.silo.results.IterableFetchResult;

public class FieldIndexResultImpl<T>
	extends IterableFetchResult<T>
	implements FieldIndexResult<T>
{
	private final long offset;
	private final long limit;
	private final long size;
	private final long total;

	public FieldIndexResultImpl(
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
