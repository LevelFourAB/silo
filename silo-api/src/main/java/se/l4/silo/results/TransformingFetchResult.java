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
	private FetchResult in;
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
	public int getLimit()
	{
		return in.getLimit();
	}
	
	@Override
	public int getOffset()
	{
		return in.getOffset();
	}
	
	@Override
	public int getSize()
	{
		return in.getSize();
	}
	
	@Override
	public int getTotal()
	{
		return in.getTotal();
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return new TransformingIterator(in.iterator());
	}
	
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
		
		@Override
		public T next()
		{
			return (T) func.apply(it.next());
		}
		
	}
}
