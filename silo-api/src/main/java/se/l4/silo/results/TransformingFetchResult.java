package se.l4.silo.results;

import java.util.Iterator;
import java.util.function.Function;

import se.l4.silo.FetchResult;

/**
 * {@link FetchResult} that transforms found entries.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class TransformingFetchResult<T>
	implements FetchResult<T>
{
	@SuppressWarnings("rawtypes")
	private FetchResult in;
	@SuppressWarnings("rawtypes")
	private Function func;

	public <I> TransformingFetchResult(FetchResult<I> in, Function<I, T> func)
	{
		this.in = in;
		this.func = func;
	}
	
	@Override
	public void close()
	{
		in.close();
	}
	
	@Override
	public boolean isEmpty()
	{
		return in.isEmpty();
	}
	
	@Override
	public long getLimit()
	{
		return in.getLimit();
	}
	
	@Override
	public long getOffset()
	{
		return in.getOffset();
	}
	
	@Override
	public long getSize()
	{
		return in.getSize();
	}
	
	@Override
	public long getTotal()
	{
		return in.getTotal();
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return new TransformingIterator(in.iterator());
	}
	
	@SuppressWarnings("rawtypes")
	private class TransformingIterator
		implements Iterator<T>
	{
		private Iterator it;

		public TransformingIterator(Iterator it)
		{
			this.it = it;
		}

		@Override
		public boolean hasNext()
		{
			return it.hasNext();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public T next()
		{
			return (T) func.apply(it.next());
		}
		
	}
}
