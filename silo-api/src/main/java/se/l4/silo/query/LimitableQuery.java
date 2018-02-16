package se.l4.silo.query;

/**
 * Extension for {@link Query} that indicates that a query can limit its
 * results.
 * 
 * @author Andreas Holstenson
 *
 */
public interface LimitableQuery<Self extends LimitableQuery<Self>>
{
	/**
	 * Set the offset of this query.
	 * 
	 * @param offset
	 * @return
	 */
	Self offset(long offset);
	
	/**
	 * Set the number of results this query can return.
	 * 
	 * @param limit
	 * @return
	 */
	Self limit(long limit);
	
	/**
	 * Paginate this query, this will invoke {@link #offset(long)} and
	 * {@link #limit(long)} with arguments calculated from the page.
	 * 
	 * @param page
	 * @param pageSize
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default Self paginate(int page, int pageSize)
	{
		if(page < 1) throw new IllegalArgumentException("page must be a positive integer");
		if(pageSize < 1) throw new IllegalArgumentException("pageSize must be a positive integer");
		
		offset((page-1) * pageSize);
		limit(pageSize);
		return (Self) this;
	}
}
