package se.l4.silo.engine.internal.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.StorageException;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.internal.StorageEngine;
import se.l4.silo.engine.internal.StorageImpl;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.engine.types.StringFieldType;
import se.l4.silo.engine.types.VersionedType;
import se.l4.ylem.io.Bytes;

/**
 * Updater that ensures that {@link QueryEngine}s have been updated with
 * the correct data.
 *
 * @author Andreas Holstenson
 *
 */
public class QueryEngineUpdater<T>
{
	private static final Logger log = LoggerFactory.getLogger(QueryEngineUpdater.class);

	private static final Long DEFAULT = 0l;

	private final StorageEngine engine;
	private final StorageImpl<T> storage;
	private final String name;

	private final MapIterable<String, EngineDef> engines;


	private final MVMap<String, Long> state;

	private final MVMap<Object[], Long> data;
	private final DataStorage dataStorage;

	public QueryEngineUpdater(
		StorageEngine engine,
		DataStorage dataStorage,
		MVStoreManager store,
		MVStoreManager stateStore,
		StorageImpl<T> storage,
		ScheduledExecutorService executor,
		String name,
		MapIterable<String, QueryEngine<T, ?>> engines
	)
	{
		this.engine = engine;
		this.storage = storage;
		this.name = name;

		this.engines = engines.collectValues((key, value) -> new EngineDef(key, value)).toImmutable();

		state = stateStore.openMap("storage.query-engine." + name,
			StringFieldType.INSTANCE,
			VersionedType.singleVersion(LongFieldType.INSTANCE)
		);

		this.dataStorage = dataStorage;

		data = store.openMap("index." + name,
			new MergedFieldType(LongFieldType.INSTANCE, StringFieldType.INSTANCE),
			LongFieldType.INSTANCE
		);

		// Check the state of all indexes and start building new ones
		long latest = storage.getLatest();

		log.debug("Updater created for storage {}, latest entry in storage is {}", name, latest);

		ensureUpToDate(executor, latest);
	}

	public void store(long previous, long id, String index, Bytes bytes)
	{
		EngineDef def = engines.get(index);
		if(def == null)
		{
			// This engine no longer exists
			// TODO: Clean up of old data?
			return;
		}

		// Store the data in the main storage
		try
		{
			long storedId = dataStorage.store(bytes);
			data.put(new Object[] { id, index }, storedId);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not update " + index + " for " + this.name + "; " + e.getMessage(), e);
		}

		// If the query engine is currently up to date perform indexing
		long lastUpdate = state.getOrDefault(def.name, DEFAULT);
		if(lastUpdate == 0 || lastUpdate == previous)
		{
			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] " + def.name + " is at " + lastUpdate + " and is up to date, updating");
			}

			// This query engine is up to date, continue indexing
			try(InputStream in0 = bytes.asInputStream();
				ExtendedDataInputStream in = new ExtendedDataInputStream(in0))
			{
				def.engine.apply(id, in);
			}
			catch(IOException e)
			{
				throw new StorageException("Could not update " + index + " for " + this.name + "; " + e.getMessage(), e);
			}

			state.put(def.name, id);
		}
		else if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] " + def.name + " is at " + lastUpdate + ", and is not up to date, skipping");
		}
	}

	public void delete(long id)
	{
		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Delete request for " + id);
		}

		for(EngineDef def : engines)
		{
			long lastUpdate = state.getOrDefault(def.name, DEFAULT);

			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] " + def.name + " is at " + lastUpdate + ", " + (lastUpdate > id ? "updating" : "skipping"));
			}

			/*
			 * Resolve the data stored for the query engine and remove it
			 * if we have it.
			 */
			try
			{
				Object[] key = new Object[] { id, def.name };
				long storedId = data.getOrDefault(key, DEFAULT);
				if(storedId > 0)
				{
					dataStorage.delete(storedId);
					data.remove(key);
				}
			}
			catch(IOException e)
			{
				throw new StorageException("Could not update " + def.name + " for " + this.name + "; " + e.getMessage(), e);
			}

			if(lastUpdate >= id)
			{
				/*
				 * If we have already indexed the item we can delete it
				 * otherwise do not nothing as it should not be indexed at
				 * all.
				 */
				def.engine.delete(id);
			}

			if(lastUpdate == id)
			{
				/*
				 * We are deleting the latest item so we need to update the
				 * state.
				 */
				state.put(def.name, lastUpdate);
			}
		}
	}

	private void ensureUpToDate(ScheduledExecutorService executor, long latest)
	{
		executor.execute(() -> {
			migrate();

			for(EngineDef def : this.engines)
			{
				long current = state.getOrDefault(def.name, DEFAULT);

				if(log.isTraceEnabled())
				{
					log.trace("  " + def.name + " is currently at " + current + ", " + (current < latest ? "updating" : "skipping update"));
				}

				if(current < latest)
				{
					executor.submit(() -> updateEngine(def));
				}
			}
		});
	}

	private void migrate()
	{
		Object[] lastKey = data.lastKey();
		if(lastKey != null && (long) lastKey[0] == storage.getLatest())
		{
			// The migration is up to date
			return;
		}

		log.info("Migrating query engines for {}", name);

		// TODO: Make this resumeable

		Iterator<LongObjectPair<T>> it = storage.iterator();
		while(it.hasNext())
		{
			LongObjectPair<T> pair = it.next();

			// Go through every engine and generate data
			for(EngineDef def : engines)
			{
				// Store the data in the main storage
				try
				{
					Bytes indexBytes = Bytes.capture(out0 -> {
						try(ExtendedDataOutputStream out = new ExtendedDataOutputStream(out0))
						{
							def.engine.generate(pair.getTwo(), out);
						}
					});

					long storedId = dataStorage.store(indexBytes);
					data.put(new Object[] { pair.getOne(), def.name }, storedId);
				}
				catch(IOException e)
				{
					throw new StorageException("Could not update " + def.name + " for " + this.name + "; " + e.getMessage(), e);
				}
			}
		}
	}

	private void updateEngine(EngineDef def)
	{
		log.info("Restoring index {} for {} ", def.name, name);

		try
		{
			long current = state.getOrDefault(def.name, DEFAULT);
			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] Running through and updating " + def.name + ", currently at " + current + " with storage at " + storage.getLatest());
			}

			// TODO: Thread safety?
			long time = System.currentTimeMillis();
			int count = 0;

			while(current < storage.getLatest() && ! Thread.interrupted())
			{
				long id = storage.nextId(current);

				if(log.isTraceEnabled())
				{
					log.trace("[" + name + "] Updating for " + id);
				}

				Long internalId = data.get(new Object[] { id, def.name });
				Bytes bytes = dataStorage.get(internalId);

				try(InputStream in0 = bytes.asInputStream();
					ExtendedDataInputStream in = new ExtendedDataInputStream(in0))
				{
					def.engine.apply(id, in);
				}
				catch(IOException e)
				{
					throw new StorageException("Could not update " + def.name + " for " + this.name + "; " + e.getMessage(), e);
				}

				state.put(def.name, id);

				current = id;
				count++;

				long now = System.currentTimeMillis();
				if(now - time >= 10000)
				{
					time = now;
					log.info("Index " + def.name + " for " + name
						+ ": Restore progress " + String.format("%.2f", (count / (double) storage.size()) * 100)
						+ "% (" + count + "/" + storage.size() + "), latest internal id is " + id);
				}
			}

			log.info("Index {} for {} is now up to date", def.name, name);
		}
		catch(Throwable t)
		{
			log.error("Index " + def.name + " for " + name + " failed to build; " + t.getMessage(), t);
		}
	}

	public boolean isAllUpDate()
	{
		long storageLatest = storage.getLatest();
		for(EngineDef def : engines)
		{
			long latest = state.getOrDefault(def.name, DEFAULT);
			if(latest != storageLatest)
			{
				return false;
			}
		}

		return true;
	}

	private class EngineDef
	{
		private final String name;
		private final QueryEngine<T, ?> engine;

		public EngineDef(String name, QueryEngine<T, ?> engine)
		{
			this.name = name;
			this.engine = engine;
		}
	}
}
