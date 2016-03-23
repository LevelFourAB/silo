package se.l4.silo.engine.internal.query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.mvstore.MVMap;

import com.google.common.collect.ImmutableList;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.internal.DataEncounterImpl;
import se.l4.silo.engine.internal.StorageImpl;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;
import se.l4.silo.engine.types.VersionedType;

public class QueryEngineUpdater
{
	private static final Long DEFAULT = 0l;
	
	private final StorageImpl storage;
	
	private final List<EngineDef> engines;
	private final MVMap<String, Long> state;

	public QueryEngineUpdater(MVStoreManager store,
			StorageImpl storage, 
			ScheduledExecutorService executor,
			String name,
			Map<String, QueryEngine<?>> engines)
	{
		this.storage = storage;
		
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
		for(EngineDef def : this.engines)
		{
			long current = state.getOrDefault(def.name, DEFAULT);
			if(current < latest)
			{
				executor.submit(() -> updateEngine(def));
			}
		}
	}
	
	public void store(long previous, long id, Bytes bytes)
	{
		for(EngineDef def : engines)
		{
			long previousForEngine = state.getOrDefault(def.name, DEFAULT);
			if(previousForEngine == 0l || previousForEngine == previous)
			{
				// This query engine is up to date, continue indexing
				def.engine.update(id, new DataEncounterImpl(bytes));
				state.put(def.name, id);
			}
		}
	}
	
	public void delete(long id)
	{
		for(EngineDef def : engines)
		{
			long latest = state.getOrDefault(def.name, DEFAULT);
			if(latest > id)
			{
				/*
				 * If we have already indexed the item we can delete it
				 * otherwise do not nothing as it should not be indexed at
				 * all.
				 */
				def.engine.delete(id);
			}
		}
	}
	
	private void updateEngine(EngineDef def)
	{
		long current = state.getOrDefault(def, DEFAULT);
		while(current < storage.getLatest() && ! Thread.interrupted())
		{
			long id = storage.nextId(current);
			
			Bytes bytes = storage.getInternal(id);
			def.engine.update(id, new DataEncounterImpl(bytes));
			state.put(def.name, id);
			
			current = id;
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
