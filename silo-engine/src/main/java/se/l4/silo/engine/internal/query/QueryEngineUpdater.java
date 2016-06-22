package se.l4.silo.engine.internal.query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import se.l4.commons.io.Bytes;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.internal.DataEncounterImpl;
import se.l4.silo.engine.internal.StorageEngine;
import se.l4.silo.engine.internal.StorageImpl;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;
import se.l4.silo.engine.types.VersionedType;

/**
 * Updater that ensures that {@link QueryEngine}s have been updated with
 * the correct data.
 * 
 * @author Andreas Holstenson
 *
 */
public class QueryEngineUpdater
{
	private static final Logger log = LoggerFactory.getLogger(QueryEngineUpdater.class);
	
	private static final Long DEFAULT = 0l;
	
	private final StorageEngine engine;
	private final StorageImpl storage;
	private final String name;
	
	private final List<EngineDef> engines;
	private final MVMap<String, Long> state;

	public QueryEngineUpdater(StorageEngine engine,
			MVStoreManager store,
			StorageImpl storage, 
			ScheduledExecutorService executor,
			String name,
			Map<String, QueryEngine<?>> engines)
	{
		this.engine = engine;
		this.storage = storage;
		this.name = name;
		
		ImmutableList.Builder<EngineDef> builder = ImmutableList.builder();
		for(Map.Entry<String, QueryEngine<?>> e : engines.entrySet())
		{
			builder.add(new EngineDef(e.getKey(), e.getValue()));
		}
		this.engines = builder.build();
		
		
		state = store.openMap("storage.query-engine." + name,
			StringFieldType.INSTANCE,
			VersionedType.singleVersion(LongFieldType.INSTANCE)
		);
		
		// Check the state of all indexes and start building new ones
		long latest = storage.getLatest();
		
		log.debug("Updater created for storage {}, latest entry in storage is {}", name, latest);
		
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
	}
	
	public void store(long previous, long id, Bytes bytes)
	{
		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Store request with previous=" + previous + ", id=" + id);
		}
		
		for(EngineDef def : engines)
		{
			long previousForEngine = state.getOrDefault(def.name, DEFAULT);
			if(previousForEngine == 0l || previousForEngine == previous)
			{
				if(log.isTraceEnabled())
				{
					log.trace("[" + name + "] " + def.name + " is at " + previousForEngine + " and is up to date, updating");
				}
				
				// This query engine is up to date, continue indexing
				def.engine.update(id, new DataEncounterImpl(engine, bytes));
				state.put(def.name, Math.max(previous, id));
			}
			else if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] " + def.name + " is at " + previousForEngine + ", and is not up to date, skipping");
			}
		}
	}
	
	public void delete(long previous, long id)
	{
		if(log.isTraceEnabled())
		{
			log.trace("[" + name + "] Delete request for " + id);
		}
		
		for(EngineDef def : engines)
		{
			long latest = state.getOrDefault(def.name, DEFAULT);
			
			if(log.isTraceEnabled())
			{
				log.trace("[" + name + "] " + def.name + " is at " + latest + ", " + (latest>id ? "updating" : "skipping"));
			}
			
			if(latest >= id)
			{
				/*
				 * If we have already indexed the item we can delete it
				 * otherwise do not nothing as it should not be indexed at
				 * all.
				 */
				def.engine.delete(id);
			}
			
			if(latest == id)
			{
				/*
				 * We are deleting the latest item so we need to update the
				 * state.
				 */
				state.put(def.name, previous);
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
				
				Bytes bytes = storage.getInternal(id);
				def.engine.update(id, new DataEncounterImpl(engine, bytes));
				state.put(def.name, id);
				
				current = id;
				count++;
				
				long now = System.currentTimeMillis();
				if(now - time >= 10000)
				{
					time = now;
					log.info("Index " + def.name + " for " + name + ": Restore progress " + count + "/" + storage.size() + " (latest internal id is " + id + ")");
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
	
	private static class EngineDef
	{
		private final String name;
		private final QueryEngine<?> engine;
		
		public EngineDef(String name, QueryEngine<?> engine)
		{
			this.name = name;
			this.engine = engine;
		}
	}
}
