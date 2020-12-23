package se.l4.silo.engine.internal;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.ObjectDataType;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;
import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.LongFieldType;

/**
 * Index that helps map objects to internal long identifiers.
 */
public class PrimaryIndex
{
	private final TransactionValue<MVMap<Object, Long>> readonlyMap;

	private final MVMap<Object, Long> map;
	private final MVMap<Long, Object> reverse;

	public PrimaryIndex(
		MVStoreManager storeManager,
		TransactionSupport transactionSupport,
		String name
	)
	{
		map = storeManager.openMap("primary.toExternal." + name, new MVMap.Builder<Object, Long>()
			.keyType(new ObjectDataType())
			.valueType(new DataTypeAdapter(LongFieldType.INSTANCE))
		);

		reverse = storeManager.openMap("primary.fromExternal." + name, new MVMap.Builder<Long, Object>()
			.keyType(new DataTypeAdapter(LongFieldType.INSTANCE))
			.valueType(new ObjectDataType())
		);

		readonlyMap = v -> map.openVersion(v);
		transactionSupport.registerValue(readonlyMap);
	}

	/**
	 * Get the long identifier of a key.
	 *
	 * @param key
	 * @return
	 */
	public long get(WriteableTransactionExchange exchange, Object key)
	{
		MVMap<Object, Long> map;
		if(exchange != null)
		{
			map = exchange.get(readonlyMap);
		}
		else
		{
			map = this.map;
		}

		Long id = map.get(key);
		return id == null ? 0 : id;
	}

	/**
	 * Store the long identifier of a key.
	 *
	 * @param key
	 * @return
	 */
	public void store(Object key, long id)
	{
		map.put(key, id);
		reverse.put(id, key);
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
