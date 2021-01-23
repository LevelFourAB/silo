package se.l4.silo.engine.internal.index.basic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.h2.mvstore.MVMap;
import org.slf4j.Logger;

import se.l4.silo.StorageException;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.index.IndexDataUpdater;
import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.engine.types.StringFieldType;

/**
 * Updater that applies previously generated data to the {@link MVMap}s used
 * by the index.
 */
public class BasicIndexUpdater
	implements IndexDataUpdater
{
	private final Logger logger;
	private final String uniqueName;

	private final MVMap<String, Long> indexState;
	private final MVMap<Long, Object[]> indexDataMap;
	private final MVMap<Object[], Object[]> indexMap;

	private final MergedFieldType dataFieldType;
	private final MergedFieldType indexData;

	private volatile long hardCommit;

	public BasicIndexUpdater(
		Logger logger,
		MVStoreManager store,

		String uniqueName,

		MergedFieldType dataFieldType,
		MVMap<Long, Object[]> indexedData,

		MergedFieldType indexData,
		MVMap<Object[], Object[]> index
	)
	{
		this.logger = logger;
		this.uniqueName = uniqueName;

		this.dataFieldType = dataFieldType;
		this.indexDataMap = indexedData;

		this.indexMap = index;
		this.indexData = indexData;

		indexState = store.openMap("state", StringFieldType.INSTANCE, LongFieldType.INSTANCE);

		// Load the hard commit
		hardCommit = indexState.getOrDefault(uniqueName, 0l);

		/*
		 * Need to keep track of commits a bit, so register an action that
		 * will keep track of the last "safe" operation that wouldn't be lost
		 * after a crash.
		 */
		store.registerCommitAction(new MVStoreManager.CommitAction()
		{
			private long commit;

			@Override
			public void preCommit()
			{
				commit = indexState.getOrDefault(uniqueName, 0l);
			}

			@Override
			public void afterCommit()
			{
				hardCommit = commit;
			}
		});
	}

	@Override
	public void clear()
	{
		indexDataMap.clear();
		indexMap.clear();
		indexState.put(uniqueName, 0l);
		hardCommit = 0;
	}

	@Override
	public long getLastHardCommit()
	{
		return hardCommit;
	}

	@Override
	public void apply(long op, long id, InputStream stream)
		throws IOException
	{
		BinaryDataInput in = BinaryDataInput.forStream(stream);

		int version = in.read();
		if(version != 0)
		{
			throw new StorageException("Unknown field index version encountered: " + version);
		}

		logger.debug("{}: Update entry for id {}", uniqueName, id);

		Object[] keyData = dataFieldType.read(in);
		Object[] sort = indexData.read(in);

		// Look up our previously indexed key to see if we need to delete it
		Object[] previousKey = indexDataMap.get(id);
		remove(previousKey, id);

		// Store the new key
		store(keyData, sort, id);

		// Store that we have applied this operation
		indexState.put(uniqueName, op);
	}

	private void store(Object[] keyData, Object[] sortData, long id)
	{
		Object[] generatedKey = new Object[keyData.length + 1];
		generatedKey[keyData.length] = id;

		if(keyData.length == 0)
		{
			// Special case for only sort data
			if(logger.isTraceEnabled())
			{
				logger.trace("  storing key=" + Arrays.toString(generatedKey) + ", sort=" + Arrays.toString(sortData));
			}
			indexMap.put(generatedKey, sortData);
		}
		else
		{
			recursiveStore(keyData, sortData, generatedKey, 0);
		}

		// Store a combined key for indexedData
		indexDataMap.put(id, keyData);
	}

	private void recursiveStore(
		Object[] keyData,
		Object[] sortData,
		Object[] generatedKey,
		int i
	)
	{
		if(i == keyData.length - 1)
		{
			for(Object o : (Object[]) keyData[i])
			{
				generatedKey[i] = o;
				if(logger.isTraceEnabled())
				{
					logger.trace("  storing key=" + Arrays.toString(generatedKey) + ", sort=" + Arrays.toString(sortData));
				}
				Object[] keyCopy = Arrays.copyOf(generatedKey, generatedKey.length);
				indexMap.put(keyCopy, sortData);
			}
		}
		else
		{
			for(Object o : (Object[]) keyData[i])
			{
				generatedKey[i] = o;
				recursiveStore(keyData, sortData, generatedKey, i + 1);
			}
		}
	}

	@Override
	public void delete(long op, long id)
	{
		logger.debug("{}: Delete entry for id {}", uniqueName, id);
		Object[] previousKey = indexDataMap.get(id);
		remove(previousKey, id);
		indexDataMap.remove(id);

		// Store that we have applied this operation
		indexState.put(uniqueName, op);
	}


	private void remove(Object[] data, long id)
	{
		if(data == null) return;

		Object[] generatedKey = new Object[data.length + 1];
		generatedKey[data.length] = id;

		if(data.length == 0)
		{
			indexMap.remove(generatedKey);
		}
		else
		{
			recursiveRemove(data, generatedKey, 0);
		}
	}

	private void recursiveRemove(Object[] data, Object[] generatedKey, int i)
	{
		Object[] values = (Object[]) data[i];
		if(i == data.length - 1)
		{
			// Last item, perform actual remove
			for(Object o : values)
			{
				generatedKey[i] = o;
				if(logger.isTraceEnabled())
				{
					logger.trace("  removing " + Arrays.toString(generatedKey));
				}
				indexMap.remove(generatedKey);
			}
		}
		else
		{
			for(Object o : values)
			{
				generatedKey[i] = o;
				recursiveRemove(data, generatedKey, i + 1);
			}
		}
	}
}
