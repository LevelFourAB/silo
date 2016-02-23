package se.l4.silo.engine.internal;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.ObjectDataType;

import se.l4.aurochs.core.id.LongIdGenerator;
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
	private final LongIdGenerator ids;

	public PrimaryIndex(MVStoreManager storeManager, LongIdGenerator ids, String name)
	{
		this.ids = ids;
		map = storeManager.openMap("primary." + name, new MVMap.Builder<Object, Long>()
			.keyType(new ObjectDataType())
			.valueType(new DataTypeAdapter(LongFieldType.INSTANCE))
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
}
