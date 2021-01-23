package se.l4.silo.engine.index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.engine.TransactionValueProvider;
import se.l4.silo.index.Query;

/**
 * Query runner that performs querying on an {@link Index}.
 */
public interface IndexQueryRunner<T, Q extends Query<T, ?, ?>>
	extends TransactionValueProvider
{
	/**
	 * Fetch some results using this engine.
	 *
	 * @param encounter
	 */
	Mono<? extends FetchResult<?>> fetch(IndexQueryEncounter<? extends Q, T> encounter);

	/**
	 *
	 * @param <R>
	 * @param encounter
	 * @return
	 */
	Flux<?> stream(IndexQueryEncounter<? extends Q, T> encounter);
}
