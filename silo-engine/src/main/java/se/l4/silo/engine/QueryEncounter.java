package se.l4.silo.engine;

import java.util.Iterator;

import com.carrotsearch.hppc.cursors.LongIntCursor;

/**
 * Encounter with a query, contains information that can be used by a
 * {@link QueryEngine} to return results.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryEncounter<T>
{
	/**
	 * Get the offset where results are expected to begin. Offsets are used
	 * to support pagination.
	 * 
	 * @return
	 */
	int getOffset();
	
	/**
	 * Get the max number of results to return.
	 * 
	 * @return
	 */
	int getMax();
	
	/**
	 * Get the data that describes the query.
	 * 
	 * @return
	 */
	T getData();
	
	/**
	 * Automatically slice an iterator with results so that the offset and
	 * maximum number of results are applied.
	 * 
	 * @param iterator
	 * @return
	 */
	Iterator<LongIntCursor> sliceResults(Iterator<LongIntCursor> iterator);
}
