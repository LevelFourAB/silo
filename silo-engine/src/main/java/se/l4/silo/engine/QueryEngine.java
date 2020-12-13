package se.l4.silo.engine;

import java.io.Closeable;
import java.io.IOException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.query.Query;

/**
 * Engine that provides query abilities for stored data.
 */
public interface QueryEngine<T, Q extends Query<T, ?, ?>>
	extends Closeable
{
	/**
	 * Get the name that this engine is available as.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Fetch some results using this engine.
	 *
	 * @param encounter
	 */
	Mono<? extends FetchResult<?>> fetch(QueryEncounter<? extends Q, T> encounter);

	/**
	 *
	 * @param <R>
	 * @param encounter
	 * @return
	 */
	Flux<?> stream(QueryEncounter<? extends Q, T> encounter);

	/**
	 * Generate data for this index. This data will be applied by the index
	 * after the current transaction is committed.
	 *
	 * @param data
	 * @param out
	 * @return
	 */
	void generate(T data, ExtendedDataOutputStream out)
		throws IOException;

	/**
	 * Apply previously generated data.
	 *
	 * @param id
	 * @param in
	 */
	void apply(long id, ExtendedDataInputStream in)
		throws IOException;

	/**
	 * Delete something from this query engine.
	 *
	 * @param id
	 */
	void delete(long id);
}
