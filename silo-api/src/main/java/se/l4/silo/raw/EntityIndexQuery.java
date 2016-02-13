package se.l4.silo.raw;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import se.l4.silo.FetchResult;

/**
 * Query on an index.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EntityIndexQuery<T>
{
	/**
	 * Set the name of the field that the operation should run on.
	 * 
	 * @param name
	 * @return
	 */
	EntityIndexQuery<T> field(String name);
	
	/**
	 * Indicate that multiple values can match.
	 * @return 
	 */
	EntityIndexQuery<T> multipleOr();
	
	/**
	 * Check if the field is equal to the specified value.
	 * 
	 * @param value
	 * @return
	 */
	EntityIndexQuery<T> isEqualTo(Object value);
	
	/**
	 * Check if the field is more than the specified number.
	 * 
	 * @param number
	 * @return
	 */
	EntityIndexQuery<T> isMoreThan(Number number);
	
	/**
	 * Check if the field is less than the specified number.
	 * 
	 * @param number
	 * @return
	 */
	EntityIndexQuery<T> isLessThan(Number number);
	
	/**
	 * Check if the field is less than or equal to the specified number.
	 * 
	 * @param number
	 * @return
	 */
	EntityIndexQuery<T> isLessThanOrEqualTo(Number number);

	/**
	 * Check if the field is more than or equal to the specified number.
	 * 
	 * @param number
	 * @return
	 */
	EntityIndexQuery<T> isMoreThanOrEqualTo(Number number);
	
	/**
	 * Sort by the specified field in ascending order.
	 * 
	 * @return
	 */
	EntityIndexQuery<T> sortAscending();
	
	/**
	 * Sort by the specified field in descending order.
	 * 
	 * @return
	 */
	EntityIndexQuery<T> sortDescending();
	
	/**
	 * Limit the number of results returned.
	 * 
	 * @param limit
	 * @return
	 */
	EntityIndexQuery<T> limit(int limit);
	
	/**
	 * Set the offset for the query.
	 * 
	 * @param intValue
	 * @return
	 */
	EntityIndexQuery<T> offset(int offset);
	
	/**
	 * Request that a count is returned for this query.
	 * 
	 * @return
	 */
	EntityIndexQuery<T> returnCount();
	
	/**
	 * Execute the query.
	 * 
	 * @return
	 */
	CompletableFuture<FetchResult<T>> runAsync();

	/**
	 * Execute the query.
	 * 
	 * @return
	 * @throws IOException 
	 */
	FetchResult<T> run();
}
