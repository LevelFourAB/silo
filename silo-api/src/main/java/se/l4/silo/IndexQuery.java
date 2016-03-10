package se.l4.silo;

import se.l4.silo.index.IndexQueryType;

/**
 * Simple query abilities that run on top of indexed fields.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface IndexQuery<T>
	extends Query<T>
{
	static <T> QueryType<T, IndexQuery<T>> type()
	{
		return new IndexQueryType<>();
	}
	
	/**
	 * Set the name of the field that the operation should run on.
	 * 
	 * @param name
	 * @return
	 */
	IndexQuery<T> field(String name);
	
	/**
	 * Check if the field is equal to the specified value.
	 * 
	 * @param value
	 * @return
	 */
	IndexQuery<T> isEqualTo(Object value);
	
	/**
	 * Check if the field is more than the specified number.
	 * 
	 * @param number
	 * @return
	 */
	IndexQuery<T> isMoreThan(Number number);
	
	/**
	 * Check if the field is less than the specified number.
	 * 
	 * @param number
	 * @return
	 */
	IndexQuery<T> isLessThan(Number number);
	
	/**
	 * Check if the field is less than or equal to the specified number.
	 * 
	 * @param number
	 * @return
	 */
	IndexQuery<T> isLessThanOrEqualTo(Number number);

	/**
	 * Check if the field is more than or equal to the specified number.
	 * 
	 * @param number
	 * @return
	 */
	IndexQuery<T> isMoreThanOrEqualTo(Number number);
	
	/**
	 * Sort by the specified field in ascending order.
	 * 
	 * @return
	 */
	IndexQuery<T> sortAscending();
	
	/**
	 * Sort by the specified field in descending order.
	 * 
	 * @return
	 */
	IndexQuery<T> sortDescending();
	
	/**
	 * Limit the number of results returned.
	 * 
	 * @param limit
	 * @return
	 */
	IndexQuery<T> limit(int limit);
	
	/**
	 * Set the offset for the query.
	 * 
	 * @param intValue
	 * @return
	 */
	IndexQuery<T> offset(int offset);
	
	/**
	 * Request that a count is returned for this query.
	 * 
	 * @return
	 */
	IndexQuery<T> returnCount();
}
