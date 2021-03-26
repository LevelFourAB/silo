package se.l4.silo.engine.index;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Data generator for a {@link Index}, used to generate data later
 * applied by {@link IndexDataUpdater}.
 */
public interface IndexDataGenerator<T>
{

	/**
	 * Generate data for this index. This data will be applied by the index
	 * after the current transaction is committed.
	 *
	 * @param data
	 * @param out
	 * @return
	 */
	void generate(T data, OutputStream out)
		throws IOException;
}
