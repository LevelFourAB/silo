package se.l4.silo.engine.search.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import se.l4.silo.search.SearchIndexException;

/**
 * Class for managing access to {@link IndexSearcher}s in a way that fits well
 * with transactions in Silo.
 *
 * <p>
 * This class encapsulates most of the transaction support and uses a lazy
 * approach. It will perform a resolve of searchers so they are only referenced
 * when needed.
 *
 * <p>
 * This is currently in two scenarios:
 *
 * <ul>
 *   <li>
 *      A search is starting, a searcher for the current generation must be
 *      created
 *   <li>
 *      A an index update is occurring, a searcher for all active transactions
 *      needs to be resolved
 * </ul>
 */
public class IndexSearcherManager
{
	/**
	 * The writer used to update the index.
	 */
	private final IndexWriter writer;

	/**
	 * Self-referencing map to keep track of the active handles.
	 */
	private final ConcurrentHashMap<HandleImpl, HandleImpl> handles;

	/**
	 * The latest version, used to keep an open searcher around for quicker
	 * searches.
	 */
	private SearcherRef latestVersion;

	/**
	 * Lock for protecting opening of searchers.
	 */
	private final Lock searcherLock;

	/**
	 * Map for keeping track of searcher references.
	 */
	private final MutableLongObjectMap<SearcherRef> searcherRefs;

	/**
	 * Boolean that keeps track of there has been deletions.
	 */
	private boolean hasDeletions;

	public IndexSearcherManager(
		IndexWriter writer
	)
	{
		this.writer = writer;

		handles = new ConcurrentHashMap<>();

		searcherLock = new ReentrantLock();
		searcherRefs = LongObjectMaps.mutable.empty();
	}

	/**
	 * Get a handle that provides access to a {@link IndexSearcher} for the
	 * current version of the index.
	 *
	 * @return
	 */
	public IndexSearcherHandle acquire()
	{
		long sequence = writer.getMaxCompletedSequenceNumber();
		HandleImpl handle = new HandleImpl(sequence);
		handles.put(handle, handle);
		return handle;

	}

	/**
	 * Indicate that a mutation is about to occur for the index. This lets
	 * this manager resolve searchers to keep read transactions available.
	 *
	 * @throws IOException
	 */
	public void willMutate(boolean isDeletion)
		throws IOException
	{
		searcherLock.lock();
		try
		{
			for(HandleImpl h : handles)
			{
				// This will resolve the searcher
				h.getSearcher();
			}

			if(isDeletion)
			{
				hasDeletions = true;
			}
		}
		finally
		{
			searcherLock.unlock();
		}
	}

	/**
	 * Indicate that changes have been committed.
	 */
	public void changesCommitted()
	{
		hasDeletions = false;
	}

	/**
	 * For testing: Get the number of handles active.
	 *
	 * @return
	 */
	int getHandleCount()
	{
		return handles.size();
	}

	/**
	 * For testing: Get the number of searchers that are around.
	 *
	 * @return
	 */
	int getSearcherRefCount()
	{
		return searcherRefs.size();
	}

	private SearcherRef findSearcher(long sequence)
		throws IOException
	{
		searcherLock.lock();
		try
		{
			if(searcherRefs.containsKey(sequence))
			{
				// Get and acquire a handle to the searcher
				SearcherRef ref = searcherRefs.get(sequence);
				ref.acquire();

				return ref;
			}
			else
			{
				/*
				 * Open a new reader and searcher on the current writer and
				 * start tracking it.
				 */
				DirectoryReader reader = DirectoryReader.open(writer, hasDeletions, false);
				SearcherRef ref = new SearcherRef(sequence, reader);

				searcherRefs.put(sequence, ref);

				/*
				 * Check if this is the latest available searcher and if so
				 * keep a reference to it around.
				 */
				if(writer.getMaxCompletedSequenceNumber() == sequence)
				{
					if(latestVersion != null)
					{
						latestVersion.release();
					}

					ref.acquire();
					latestVersion = ref;
				}

				return ref;
			}
		}
		finally
		{
			searcherLock.unlock();
		}
	}

	public void close()
		throws IOException
	{
		for(SearcherRef ref : searcherRefs)
		{
			ref.close();
		}
	}

	private class HandleImpl
		implements IndexSearcherHandle
	{
		private final long sequence;
		private volatile SearcherRef ref;

		public HandleImpl(
			long sequence
		)
		{
			this.sequence = sequence;
		}

		@Override
		public IndexSearcher getSearcher()
			throws IOException
		{
			SearcherRef ref = this.ref;
			if(ref != null) return ref.searcher;

			searcherLock.lock();
			try
			{
				/*
				 * This will wait for the lock to become available so it
				 * may or may not have resolved a reference if this is used
				 * from different threads so need to do a check.
				 */

				if(this.ref == null)
				{
					// If still no reference create it
					this.ref = ref = findSearcher(sequence);
				}
				else
				{
					// If there is now a reference use it
					ref = this.ref;
				}
			}
			finally
			{
				searcherLock.unlock();
			}

			return ref.searcher;
		}

		@Override
		public void release()
		{
			SearcherRef reference = this.ref;
			if(reference != null)
			{
				// If we have a searcher we release it
				try
				{
					reference.release();
				}
				catch(IOException e)
				{
					throw new SearchIndexException("Could not release index; " + e.getMessage(), e);
				}
			}

			// Remove from the list of active handles
			handles.remove(this);
		}
	}

	private class SearcherRef
	{
		private final long sequence;
		private final IndexReader reader;
		private final IndexSearcher searcher;
		private final AtomicInteger count;

		public SearcherRef(
			long sequence,
			IndexReader reader
		)
		{
			this.sequence = sequence;
			this.reader = reader;
			this.searcher = new IndexSearcher(reader);
			this.count = new AtomicInteger(1);
		}

		public void acquire()
		{
			count.incrementAndGet();
		}

		public void release()
			throws IOException
		{
			if(count.decrementAndGet() == 0)
			{
				reader.close();

				searcherLock.lock();
				try
				{
					searcherRefs.removeKey(sequence);
				}
				finally
				{
					searcherLock.unlock();
				}
			}
		}

		public void close()
			throws IOException
		{
			reader.close();
		}
	}
}
