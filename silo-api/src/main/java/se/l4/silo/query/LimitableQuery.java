package se.l4.silo.query;

import java.util.OptionalLong;

/**
 * Extension for {@link Query} that indicates that a query can limit its
 * results.
 */
public interface LimitableQuery
{
	/**
	 * Get the offset of this query.
	 *
	 * @return
	 */
	OptionalLong getResultOffset();

	/**
	 * Get the limit of this query.
	 *
	 * @return
	 */
	OptionalLong getResultLimit();

	/**
	 * Builder that supports limiting the range of results.
	 */
	public interface Builder<Self extends Builder<Self>>
	{
		/**
		 * Set the offset of this query.
		 *
		 * @param offset
		 *   the offset to start returning from
		 * @return
		 *   copy of this builder with an offset set
		 */
		Self offset(long offset);

		/**
		 * Set the number of results this query can return.
		 *
		 * @param limit
		 *   number of results to limit to
		 * @return
		 *   copy of this builder with a limit set
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
		default Self paginate(int page, int pageSize)
		{
			if(page < 1) throw new IllegalArgumentException("page must be a positive integer");
			if(pageSize < 1) throw new IllegalArgumentException("pageSize must be a positive integer");

			return offset((page-1) * pageSize)
				.limit(pageSize);
		}
	}
}
