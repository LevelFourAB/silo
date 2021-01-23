package se.l4.silo.engine.index;

import java.io.Closeable;

import se.l4.silo.index.Query;

/**
 * Index that indexes and provides query abilities for stored data.
 */
public interface Index<T, Q extends Query<T, ?, ?>>
	extends Closeable
{
	/**
	 * Get the name that this engine is available as.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Create a generator for this index. This generator is responsible for
	 * creating data that will later be applied to the index.
	 *
	 * @return
	 */
	IndexDataGenerator<T> getDataGenerator();

	/**
	 * Create an updater for this index.
	 *
	 * @return
	 */
	IndexDataUpdater getDataUpdater();

	/**
	 * Create a query runner for this index.
	 *
	 * @return
	 */
	IndexQueryRunner<T, Q> getQueryRunner();
}
