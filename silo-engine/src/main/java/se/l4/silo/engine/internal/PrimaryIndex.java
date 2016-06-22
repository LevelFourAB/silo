package se.l4.silo.engine.internal;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.ObjectDataType;

import se.l4.commons.id.LongIdGenerator;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.LongFieldType;

/**
 * Index that helps map objects to internal long identifiers.
 * 
 * @author Andreas Holstenson
 *
 */
public class PrimaryIndex
{
	private final MVMap<Object, Long> map;
	private final MVMap<Long, Object> reverse;
	
	private final LongIdGenerator ids;

	public PrimaryIndex(MVStoreManager storeManager, LongIdGenerator ids, String name)
	{
		this.ids = ids;
		map = storeManager.openMap("primary.toExternal." + name, new MVMap.Builder<Object, Long>()
			.keyType(new ObjectDataType())
			.valueType(new DataTypeAdapter(LongFieldType.INSTANCE))
		);
		
		reverse = storeManager.openMap("primary.fromExternal." + name, new MVMap.Builder<Long, Object>()
			.keyType(new DataTypeAdapter(LongFieldType.INSTANCE))
			.valueType(new ObjectDataType())
		);
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
		map.put(key, id);
		reverse.put(id, key);
		return id;
	}
	
	/**
	 * Remove the long identifier associated with the key.
	 * 
	 * @param key
	 */
	public void remove(Object key)
	{
		Long removed = map.remove(key);
		if(removed != null)
		{
			reverse.remove(removed);
		}
	}
	
	public long first()
	{
		Long first = reverse.firstKey();
		return first == null ? 0l : first;
	}
	
	/**
	 * Get the latest internal identifier.
	 * 
	 * @return
	 */
	public long latest()
	{
		Long last = reverse.lastKey();
		return last == null ? 0l : last;
	}
	
	/**
	 * Get the next id in use after the given id.
	 * 
	 * @param id
	 * @return
	 */
	public long nextAfter(long id)
	{
		Long higher = reverse.higherKey(id);
		return higher == null ? 0l : higher;
	}
	
	public long before(long id)
	{
		Long lower = reverse.lowerKey(id);
		return lower == null ? 0l : lower;
	}

	public int size()
	{
		return map.size();
	}
}
