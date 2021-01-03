package se.l4.silo.engine;

/**
 * Event that can occur on an index.
 */
public interface IndexEvent
{
	/**
	 * {@link IndexEvent} for rebuild progress. This is emitted when an index
	 * is being rebuilt, either because of a crash, it being a new index or
	 * because it has been requested.
	 */
	interface RebuildProgress
		extends IndexEvent
	{
		/**
		 * Get if index is currently queryable.
		 *
		 * @return
		 *   {@code true} if the index can be queried without delay, otherwise
		 *   {@code false}
		 */
		boolean isQueryable();

		/**
		 * Get the current progress.
		 *
		 * @return
		 */
		long getProgress();

		/**
		 * Get the total items being rebuilt.
		 *
		 * @return
		 */
		long getTotal();
	}

	/**
	 * Event emitted when an index becomes queryable.
	 */
	interface Queryable
		extends IndexEvent
	{
	}

	/**
	 * Event emitted when an index becomes up to date.
	 */
	interface UpToDate
		extends IndexEvent
	{
	}
}
