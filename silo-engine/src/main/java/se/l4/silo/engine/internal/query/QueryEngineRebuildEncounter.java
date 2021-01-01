package se.l4.silo.engine.internal.query;

import java.util.Iterator;

import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

public interface QueryEngineRebuildEncounter<T>
{
	/**
	 * Get the number of items currently stored.
	 *
	 * @return
	 */
	long getSize();

	/**
	 * Get the largest id currently stored.
	 *
	 * @return
	 */
	long getLargestId();

	/**
	 * Get an iterator that will iterate over data from the given stored
	 * id (exclusive).
	 *
	 * @param dataId
	 * @return
	 */
	Iterator<LongObjectPair<T>> iterator(long minIdExclusive, long maxIdInclusive);

	/**
	 * Report some progress in the rebuild.
	 *
	 * @param dataId
	 *   the maximum data id being rebuilt
	 */
	void reportProgress(long dataId);


	class State
	{
		private final long size;
		private final long maximumData;

		public State(
			long size,
			long maximumData
		)
		{
			this.size = size;
			this.maximumData = maximumData;
		}

		public long getSize()
		{
			return size;
		}

		public long getMaximumData()
		{
			return maximumData;
		}
	}
}
