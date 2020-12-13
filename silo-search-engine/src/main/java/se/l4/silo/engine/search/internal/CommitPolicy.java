package se.l4.silo.engine.search.internal;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

/**
 * Helper that handles committing a {@link IndexWriter} based on a preset
 * configuration.
 */
public class CommitPolicy
{
	private final Logger logger;
	private final String name;

	private final IndexWriter writer;
	private final int maxDocuments;
	private final long maxTime;

	private final AtomicLong count;
	private final AtomicLong lastCommit;

	private final ScheduledExecutorService executor;

	private volatile Future<?> future;

	public CommitPolicy(
		Logger logger,
		String name,
		ScheduledExecutorService executor,
		IndexWriter writer,
		int maxDocumentChanges,
		long maxTimeBetweenCommits
	)
	{
		this.logger = logger;
		this.name = name;
		this.executor = executor;
		this.writer = writer;
		this.maxDocuments = maxDocumentChanges;
		this.maxTime = maxTimeBetweenCommits;

		count = new AtomicLong();
		lastCommit = new AtomicLong();
	}

	public void indexModified()
	{
		increment();
	}

	public void commit()
		throws IOException
	{
		commit(false);
	}

	private void commit(boolean fromThread)
		throws IOException
	{
		logger.debug("{}: Committing index updates", name);
		long t1 = System.currentTimeMillis();
		writer.commit();
		long t2 = System.currentTimeMillis();
		lastCommit.set(t2);

		if(logger.isDebugEnabled())
		{
			logger.debug("{}, Commit took {} ms", name, (t2-t1));
		}

		synchronized(this)
		{
			if(! fromThread && future != null)
			{
				future.cancel(false);
				future = null;
			}
		}
	}

	private void increment()
	{
		if(count.incrementAndGet() % maxDocuments == 0)
		{
			try
			{
				commit();
			}
			catch(IOException e)
			{
			}
		}

		scheduleCommit();
	}

	private void scheduleCommit()
	{
		synchronized(this)
		{
			if(future == null)
			{
				future = executor.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							commit(true);
							future = null;
						}
						catch(IOException e)
						{
							logger.error("Unable to commit; " + e.getMessage(), e);
						}

					}
				}, maxTime, TimeUnit.SECONDS);
			}
		}
	}

	public void close()
	{
		if(future != null)
		{
			future.cancel(false);
			future = null;
		}
	}
}
