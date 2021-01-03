package se.l4.silo.engine.internal.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.IndexEvent;
import se.l4.silo.engine.LocalIndex;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.internal.DataStorage;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.query.Query;

/**
 * Controller for instances of {@link QueryEngine} that keeps these up to date
 * with the stored data.
 */
public class QueryEngineController<T, Q extends Query<T, ?, ?>>
	implements LocalIndex
{
	private static final long PROGRESS_EVENT_INTERVAL = 10 * 1000;

	private final Logger logger;
	private final DataStorage dataStorage;

	private final QueryEngine<T, Q> engine;
	private final QueryEngineLog engineLog;
	private final MVMap<Long, Long> data;

	private final Sinks.One<Void> upToDate;
	private final LongAdder queryCount;

	private final Sinks.Many<IndexEvent> events;

	private long softCursor;
	private long lastProgressEvent;

	public QueryEngineController(
		MVStoreManager manager,
		DataStorage dataStorage,
		QueryEngine<T, Q> engine,
		String uniqueName
	)
	{
		logger = LoggerFactory.getLogger(QueryEngine.class.getName() + "." + uniqueName);

		this.dataStorage = dataStorage;
		this.engine = engine;

		this.engineLog = new QueryEngineLog(manager, "index.log." + uniqueName);
		data = manager.openMap("index.data." + uniqueName, LongFieldType.INSTANCE, LongFieldType.INSTANCE);

		events = Sinks.many().multicast().directBestEffort();
		upToDate = Sinks.one();
		queryCount = new LongAdder();
	}

	@Override
	public String getName()
	{
		return engine.getName();
	}

	@Override
	public long getQueryCount()
	{
		return queryCount.sum();
	}

	@Override
	public boolean isQueryable()
	{
		return isUpToDate();
	}

	@Override
	public Mono<Void> whenQueryable()
	{
		return whenUpToDate();
	}

	@Override
	public boolean isUpToDate()
	{
		return isCurrent();
	}

	@Override
	public Mono<Void> whenUpToDate()
	{
		return upToDate.asMono();
	}

	@Override
	public Flux<IndexEvent> events()
	{
		return events.asFlux();
	}

	public QueryEngine<T, Q> getEngine()
	{
		return engine;
	}

	/**
	 * Get if this engine is up to date and persisted.
	 *
	 * @return
	 */
	public boolean isPersisted()
	{
		return softCursor == engineLog.getLastHardCommit();
	}

	/**
	 * Get if the data in the engine is current.
	 *
	 * @return
	 */
	public boolean isCurrent()
	{
		return softCursor == engineLog.getLatestOp();
	}

	/**
	 * Start this controller.
	 *
	 * @throws IOException
	 */
	public void start(
		QueryEngineRebuildEncounter<T> encounter
	)
	{
		try
		{
			softCursor = engine.getLastHardCommit();

			if(softCursor > engineLog.getLatestOp())
			{
				/*
				* Index has ended up in a state where it has data that does not
				* exist in the main storage. The common cause here is that a
				* crash occurred or a backup was restored without removing the
				* indexes.
				*
				* As deletes may have happened in the index but not the main
				* storage, causing those items to not be queryable in the index,
				* we have to clear the index at this point and do a rebuild.
				*/
				engine.clear();
				softCursor = engine.getLastHardCommit();
			}

			if(softCursor == 0)
			{
				/*
				* Index has nothing committed so about to attempt a full rebuild
				* of the index. In this case we can't rely on the log so we need
				* to rebuild from the stored data.
				*
				* The biggest challenge here is if we're in the middle of building
				* up the data map and a restart occurs where the index has not
				* committed anything.
				*
				* To solve this we keep a generation pointer around and rebuild
				* up to where we have generated.
				*/
				long generationPointer = getGenerationPointer(engineLog.getRebuildMax());
				engineLog.clear();

				// No maximum rebuild set
				long size = encounter.getSize();
				long largestKey = encounter.getLargestId();
				if(size > 0)
				{
					engineLog.setRebuildMax(size, largestKey);
				}

				if(generationPointer > 0)
				{
					// Replay the data up to where we have generated
					replayDataLog(encounter, 0, generationPointer);
				}
			}

			// Update the hard commit
			engineLog.setLastHardCommit(softCursor);

			// First step is to rebuild via the log
			long lastOp = engineLog.getLastRebuildOp();
			replayLog(lastOp);

			if(data.size() < engineLog.getRebuildMax())
			{
				// Data is being generated or needs to be generated
				generateData(encounter);
			}
			else
			{
				// Rebuild the remaining stored data
				long lastStored = engineLog.getLastStoredId(softCursor);
				long largestDataToRebuild = engineLog.getRebuildMaxDataId();
				replayDataLog(encounter, lastStored, largestDataToRebuild);
			}

			// Replay the rest of the log
			replayLog(Long.MAX_VALUE);

			// Emit the last rebuild progress event
			if(lastProgressEvent > 0)
			{
				long last = engineLog.getLatestOp();
				events.tryEmitNext(new RebuildProgressImpl(isQueryable(), last, last));
			}

			events.tryEmitNext(new QueryableImpl());
			events.tryEmitNext(new UpToDateImpl());

			// Emit the up to date event
			upToDate.tryEmitEmpty();
		}
		catch(IOException e)
		{
			throw new StorageException("Could not update index " + engine.getName() + "; " + e.getMessage(), e);
		}
	}

	private long getGenerationPointer(long maxId)
	{
		Long key = data.floorKey(maxId);
		return key == null ? 0 : key;
	}

	private void generateData(
		QueryEngineRebuildEncounter<T> encounter
	)
		throws IOException
	{
		long current = getGenerationPointer(engineLog.getRebuildMaxDataId());

		Iterator<LongObjectPair<T>> it = encounter.iterator(current, engineLog.getRebuildMaxDataId());
		while(it.hasNext())
		{
			LongObjectPair<T> pair = it.next();

			long dataId = pair.getOne();
			T object = pair.getTwo();

			long storedId;
			try
			{
				storedId = dataStorage.store(out -> generate(object, out));
				data.put(dataId, storedId);

				// Update the index
				long opId = engineLog.appendRebuild(dataId, storedId);
				try(InputStream storedStream = dataStorage.get(null, storedId))
				{
					engine.apply(opId, dataId, new ExtendedDataInputStream(storedStream));
				}

				// Update the where we are in the log
				softCursor = opId;

				// Report our progress
				maybeEmitProgress(opId);
			}
			catch(IOException e)
			{
				throw new StorageException("Could not update " + engine.getName() + "; " + e.getMessage(), e);
			}
		}
	}

	private void replayDataLog(
		QueryEngineRebuildEncounter<T> encounter,
		long previousDataId,
		long largestDataToRebuild
	)
		throws IOException
	{
		Long largerKey = data.higherKey(previousDataId);
		Iterator<Long> it = data.keyIterator(largerKey);
		while(it.hasNext())
		{
			Long dataId = it.next();
			if(dataId > largestDataToRebuild) break;

			long storedId = data.get(dataId);
			long opId = engineLog.appendRebuild(dataId, storedId);
			try(InputStream storedStream = dataStorage.get(null, storedId))
			{
				// Protect against the data having been removed
				if(storedStream == null) continue;

				engine.apply(opId, dataId, new ExtendedDataInputStream(storedStream));
			}

			// Update the where we are in the log
			softCursor = opId;

			// Report our progress
			maybeEmitProgress(opId);
		}
	}

	private void replayLog(
		long maxOp
	)
		throws IOException
	{
		Iterator<LongObjectPair<QueryEngineLog.Entry>> it = engineLog.iterator(softCursor);

		while(it.hasNext())
		{
			LongObjectPair<QueryEngineLog.Entry> e = it.next();
			long opId = e.getOne();

			QueryEngineLog.Entry entry = e.getTwo();

			switch(entry.getType())
			{
				case STORE:
					try(InputStream in = dataStorage.get(
						null,
						entry.getIndexDataId()
					))
					{
						if(in == null)
						{
							// This data is no longer available, so no need to replay it
							if(logger.isTraceEnabled())
							{
								logger.trace("Not replaying store of {}, data is no longer available", entry.getId());
							}
						}
						else
						{
							engine.apply(opId, entry.getId(),  new ExtendedDataInputStream(in));
						}
					}
					break;
				case DELETION:
					engine.delete(opId, entry.getId());
					break;
				default:
					throw new StorageException("Unexpected type of data in log");
			}

			// Keep track of where we are up to
			softCursor = opId;

			// Report our progress
			maybeEmitProgress(opId);
		}
	}

	/**
	 * Check the current time and maybe emit an rebuild event.
	 *
	 * @param current
	 */
	private void maybeEmitProgress(long current)
	{
		long time = lastProgressEvent;
		long now = System.currentTimeMillis();
		if(now - time > PROGRESS_EVENT_INTERVAL)
		{
			long total = engineLog.getLatestOp();
			events.tryEmitNext(new RebuildProgressImpl(isQueryable(), current, total));

			lastProgressEvent = now;
		}
	}

	/**
	 * Generate data for the given object and write it to an
	 * {@link OutputStream}.
	 *
	 * @param data
	 * @param out0
	 * @throws IOException
	 */
	public void generate(T data, OutputStream out0)
		throws IOException
	{
		try(ExtendedDataOutputStream out = new ExtendedDataOutputStream(out0))
		{
			engine.generate(data, out);
		}
	}

	/**
	 * Apply a store operation to this index.
	 *
	 * @param id
	 * @param in
	 * @throws IOException
	 */
	public void store(long id, InputStream in)
	{
		long storedId;
		try
		{
			storedId = dataStorage.store(in::transferTo);
			data.put(id, storedId);

			long opId = engineLog.appendStore(id, storedId);

			if(softCursor == opId - 1)
			{
				try(InputStream storedStream = dataStorage.get(null, storedId))
				{
					engine.apply(opId, id, new ExtendedDataInputStream(storedStream));
				}

				softCursor = opId;
			}
		}
		catch(IOException e)
		{
			throw new StorageException("Could not update " + engine.getName() + "; " + e.getMessage(), e);
		}
	}

	/**
	 * Apply a delete operation to this index.
	 *
	 * @param id
	 */
	public void delete(long id)
	{
		// Remove stored data
		data.remove(id);

		// Generate an operation and apply it to the engine
		long opId = engineLog.appendDelete(id);

		if(softCursor == opId - 1)
		{
			engine.delete(opId, id);

			softCursor = opId;
		}
	}

	/**
	 * Perform a fetch on the index.
	 *
	 * @param <R>
	 * @param <FR>
	 * @param encounter
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <R, FR extends FetchResult<R>> Mono<FR> fetch(
		QueryEncounter encounter
	)
	{
		return Mono.fromRunnable(queryCount::increment)
			.then(engine.fetch(encounter));
	}

	/**
	 * Perform a stream on the index.
	 *
	 * @param <R>
	 * @param encounter
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <R> Flux<R> stream(
		QueryEncounter encounter
	)
	{
		return Mono.fromRunnable(queryCount::increment)
			.thenMany(engine.stream(encounter));
	}

	private static class RebuildProgressImpl
		implements IndexEvent.RebuildProgress
	{
		private final boolean queryable;
		private final long progress;
		private final long total;

		public RebuildProgressImpl(
			boolean queryable,
			long progress,
			long total
		)
		{
			this.queryable = queryable;
			this.progress = progress;
			this.total = total;
		}

		@Override
		public boolean isQueryable()
		{
			return queryable;
		}

		@Override
		public long getProgress()
		{
			return progress;
		}

		@Override
		public long getTotal()
		{
			return total;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(progress, queryable, total);
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj) return true;
			if(obj == null) return false;
			if(getClass() != obj.getClass()) return false;
			RebuildProgressImpl other = (RebuildProgressImpl) obj;
			return progress == other.progress
				&& queryable == other.queryable
				&& total == other.total;
		}

		@Override
		public String toString()
		{
			return "RebuildProgress{progress=" + progress + ", total=" + total
				+ ", queryable=" + queryable + "}";
		}
	}

	private static class QueryableImpl
		implements IndexEvent.Queryable
	{
		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof QueryableImpl;
		}

		@Override
		public int hashCode()
		{
			return getClass().hashCode();
		}

		@Override
		public String toString()
		{
			return "Queryable{}";
		}
	}

	private static class UpToDateImpl
		implements IndexEvent.UpToDate
	{
		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof UpToDateImpl;
		}

		@Override
		public int hashCode()
		{
			return getClass().hashCode();
		}

		@Override
		public String toString()
		{
			return "UpToDate{}";
		}
	}
}
