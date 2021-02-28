package se.l4.silo;

import java.util.Iterator;
import java.util.Spliterator;

import org.eclipse.collections.api.list.ListIterable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.results.FetchResultSpliterator;

/**
 * Result of a fetch request, contains the results and information about the
 * parameters used for fetching.
 *
 * @param <T>
 */
public interface FetchResult<T>
	extends Iterable<T>
{
	/**
	 * Get the first entry is this result.
	 *
	 * @return
	 */
	default Mono<T> first()
	{
		return Mono.fromSupplier(() -> {
			Iterator<T> it = iterator();
			if(it.hasNext())
			{
				return it.next();
			}

			return null;
		});
	}

	/**
	 * Get a {@link Flux} of the results.
	 *
	 * @return
	 */
	default Flux<T> stream()
	{
		return Flux.fromIterable(this);
	}

	@Override
	default Spliterator<T> spliterator()
	{
		return new FetchResultSpliterator<T>(this);
	}

	/**
	 * Get the fetched items as a {@link ListIterable}.
	 *
	 * @return
	 */
	ListIterable<T> getItems();
}
