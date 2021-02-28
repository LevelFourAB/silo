package se.l4.silo.results;

import java.util.Iterator;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.FetchResult;

/**
 * {@link FetchResult} implemented over an {@link Iterable}.
 *
 * @param <T>
 */
public class IterableFetchResult<T>
	implements SizeAwareResult<T>
{
	protected ListIterable<T> iterable;

	public IterableFetchResult(ListIterable<T> iterable)
	{
		this.iterable = iterable;
	}

	@Override
	public Iterator<T> iterator()
	{
		return iterable.iterator();
	}

	@Override
	public long getSize()
	{
		return iterable.size();
	}

	@Override
	public ListIterable<T> getItems()
	{
		return iterable;
	}
}
