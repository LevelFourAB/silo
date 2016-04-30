package se.l4.silo.results;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import se.l4.silo.FetchResult;

/**
 * {@link Spliterator} for any {@link FetchResult}.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class FetchResultSpliterator<T>
	extends AbstractSpliterator<T>
{
	private final Iterator<T> it;

	public FetchResultSpliterator(FetchResult<T> fr)
	{
		super(fr.getSize() < 0 ? Integer.MAX_VALUE : fr.getSize(), Spliterator.ORDERED | Spliterator.IMMUTABLE);
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
}
