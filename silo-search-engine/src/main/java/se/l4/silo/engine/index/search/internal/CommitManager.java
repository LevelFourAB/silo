package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

/**
 * Helper that handles committing a {@link IndexWriter} based on a preset
 * configuration.
 */
public class CommitManager
{
	private final Logger logger;
	private final String name;

	private final IndexWriter writer;
	private final int maxDocuments;
	private final long maxTime;

	private final AtomicLong count;
	private final Scheduler scheduler;
	private final Runnable onCommit;

	private volatile Disposable scheduled;

	private volatile long latestOp;
	private volatile long hardCommitOp;

	public CommitManager(
		Logger logger,
		String name,
		Scheduler scheduler,
		IndexWriter writer,
		int maxDocumentChanges,
		long maxTimeBetweenCommits,
		IndexSearcherManager searcherManager
	)
		throws IOException
	{
		this.logger = logger;
		this.name = name;
		this.scheduler = scheduler;
		this.writer = writer;
		this.maxDocuments = maxDocumentChanges;
		this.maxTime = maxTimeBetweenCommits;
		this.onCommit = searcherManager::changesCommitted;

		count = new AtomicLong();
		if(writer.getDocStats().numDocs == 0)
		{
			// This is a fresh index, add our commit tracker
			reinitialize();
		}
		else
		{
			// Need to load
			IndexSearcherHandle handle = searcherManager.acquire();
			try
			{
				TopDocs td = handle.getSearcher()
					.search(new TermQuery(new Term("_:type", "op")), 1);

				if(td.totalHits.value != 1l)
				{
					logger.warn("Index corrupt, could not find last operation applied. Index will be rebuilt");
					writer.deleteAll();
					reinitialize();
				}
				else
				{
					hardCommitOp = handle.getSearcher().doc(td.scoreDocs[0].doc)
						.getField("_:op")
						.numericValue()
						.longValue();
				}
			}
			finally
			{
				handle.release();
			}
		}
	}

	public void reinitialize()
		throws IOException
	{
		latestOp = 0;
		hardCommitOp = 0;
		writer.addDocument(getOpDoc(0));
	}

	private Document getOpDoc(long op)
	{
		Document doc = new Document();

		FieldType ft = new FieldType();
		ft.setStored(false);
		ft.setTokenized(false);
		ft.setIndexOptions(IndexOptions.DOCS);
		doc.add(new Field("_:type", "op", ft));

		doc.add(new StoredField("_:op", op));

		return doc;
	}

	public long getHardCommit()
	{
		return hardCommitOp;
	}

	public void indexModified(long opId)
	{
		latestOp = opId;
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

		// Keep track of the latest operation
		long op = latestOp;
		writer.updateDocument(new Term("_:type", "op"), getOpDoc(op));

		// Perform commit
		writer.commit();

		// Reference
		hardCommitOp = op;

		long t2 = System.currentTimeMillis();

		if(logger.isDebugEnabled())
		{
			logger.debug("{}, Commit took {} ms", name, (t2-t1));
		}

		onCommit.run();

		synchronized(this)
		{
			if(! fromThread && scheduled != null)
			{
				scheduled.dispose();
				scheduled = null;
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
			if(scheduled == null)
			{
				scheduled = scheduler.schedule(() -> {
					try
					{
						commit(true);
						scheduled = null;
					}
					catch(IOException e)
					{
						logger.error("Unable to commit; " + e.getMessage(), e);
					}
				}, maxTime, TimeUnit.SECONDS);
			}
		}
	}

	public void close()
	{
		if(scheduled != null)
		{
			scheduled.dispose();
			scheduled = null;
		}
	}
}
