package se.l4.silo.engine.internal;

import java.io.IOException;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.engine.DataStorage;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.config.EntityConfig;

/**
 * Entity within a {@link StorageEngine}. Entities store binary data and
 * may contain {@link QueryEngine query support}.
 * 
 * @author Andreas Holstenson
 *
 */
public class StorageEntity
{
	private final PrimaryIndex primary;
	private final DataStorage storage;

	public StorageEntity(DataStorage storage, PrimaryIndex primary)
	{
		this.storage = storage;
		this.primary = primary;
	}
	
	public void loadConfig(EntityConfig ec)
	{
	}
	
	/**
	 * Store an entry for this entity.
	 * 
	 * @param id
	 * @param bytes
	 * @throws IOException
	 */
	public void store(Object id, Bytes bytes)
		throws IOException
	{
		long internalId = primary.store(id);
		storage.store(internalId, bytes);
	}
	
	/**
	 * Get a previously stored entry.
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public Bytes get(Object id)
		throws IOException
	{
		long internalId = primary.get(id);
		if(internalId == 0) return null;
		
		return storage.get(internalId);
	}
	
	/**
	 * Delete a previously stored entry.
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void delete(Object id)
		throws IOException
	{
		long internalId = primary.get(id);
		if(internalId == 0) return;
		
		storage.delete(internalId);
		primary.remove(id);
	}
}
