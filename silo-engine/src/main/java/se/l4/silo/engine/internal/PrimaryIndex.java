package se.l4.silo.engine.internal;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.ObjectDataType;

import se.l4.aurochs.core.id.LongIdGenerator;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;

/**
 * Index that helps map objects to internal long identifiers.
 * 
 * @author Andreas Holstenson
 *
 */
public class PrimaryIndex
{
	private final MVMap<Object, Long> map;
	private final LongIdGenerator ids;
	private final MVMap<String, Long> counter;
	private final String name;

	public PrimaryIndex(MVStoreManager storeManager, LongIdGenerator ids, String name)
	{
		this.ids = ids;
		this.name = name;
		map = storeManager.openMap("primary.values." + name, new MVMap.Builder<Object, Long>()
			.keyType(new ObjectDataType())
			.valueType(new DataTypeAdapter(LongFieldType.INSTANCE))
		);
		
		counter = storeManager.openMap("primary.counter", StringFieldType.INSTANCE, LongFieldType.INSTANCE);
	}
	
	/**
	 * Get the long identifier of a key.
	 * 
	 * @param key
	 * @return
	 */
	public long get(Object key)
	{
		Long id = map.get(key);
		return id == null ? 0 : id;
	}
	
	/**
	 * Store the long identifier of a key.
	 * 
	 * @param key
	 * @return
	 */
	public long store(Object key)
	{
		if(map.containsKey(key))
		{
			return map.get(key);
		}
		
		long id = ids.next();
		counter.put(name, id);
		map.put(key, id);
		return id;
	}
	
	/**
	 * Remove the long identifier associated with the key.
	 * 
	 * @param key
	 */
	public void remove(Object key)
	{
		map.remove(key);
	}
	
	/**
	 * Get the latest internal identifier.
	 * 
	 * @return
	 */
	public long latest()
	{
		return counter.getOrDefault(name, 0l);
	}
}
