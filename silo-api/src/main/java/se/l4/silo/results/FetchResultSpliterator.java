package se.l4.silo.results;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import se.l4.silo.FetchResult;

/**
 * {@link Spliterator} for any {@link FetchResult}.
 *
 * @param <T>
 */
public class FetchResultSpliterator<T>
	extends AbstractSpliterator<T>
{
	private final Iterator<T> it;

	public FetchResultSpliterator(FetchResult<T> fr)
	{
		super(getSize(fr), Spliterator.ORDERED | Spliterator.IMMUTABLE);
		it = fr.iterator();
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action)
	{
		if(! it.hasNext()) return false;

		T next = it.next();
		action.accept(next);

		return true;
	}

	private static long getSize(FetchResult<?> fr)
	{
		if(fr instanceof SizeAwareResult)
		{
			long size = ((SizeAwareResult<?>) fr).getSize();
			return size < 0 ? Integer.MAX_VALUE : size;
		}

		return Integer.MAX_VALUE;
	}
}
