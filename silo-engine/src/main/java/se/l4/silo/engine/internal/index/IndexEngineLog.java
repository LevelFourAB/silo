package se.l4.silo.engine.internal.index;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.EmptyIterator;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.StorageException;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.internal.DataStorage;
import se.l4.silo.engine.internal.types.PositiveLongType;

/**
 * Log for keeping track of actions for a {@link Index}. This is used to
 * replay actions if a query engine stops before a hard commit of all data
 * occurs.
 *
 * <p>
 * The log is split into two parts, one virtual part to help with updates
 * from the index data and one part that keeps track of additional stores and
 * deletions.
 *
 * <p>
 * The virtual part consists of a {@link EntryType#REBUILD_MAX} entry that
 * indicates where the log would be when a rebuild from data is done, this
 * allows the log to generate identifiers larger than this value for new
 * appends.
 *
 * <p>
 * The rest of the log is a set of entries of type {@link EntryType#STORE} and
 * {@link EntryType#DELETION} that indicates operations that should be applied
 * to the engine.
 */
public class IndexEngineLog
{
	private final MVMap<Long, Entry> data;
	private final LongConsumer onStoredIdDiscarded;

	private long rebuildMax;

	public IndexEngineLog(
		MVStoreManager manager,
		String name,
		LongConsumer onStoredIdDiscarded
	)
	{
		this.onStoredIdDiscarded = onStoredIdDiscarded;
		data = manager.openMap(name, new MVMap.Builder<Long, Entry>()
			.keyType(PositiveLongType.INSTANCE)
			.valueType(EntryFieldType.INSTANCE)
		);

		if(data.isEmpty())
		{
			data.put(0l, new Entry(EntryType.HARD_COMMIT, 0, 0));
		}
		else
		{
			Iterator<Long> it = data.keyIterator(0l);
			while(it.hasNext())
			{
				Long l = it.next();
				Entry e = data.get(l);
				if(e.getType() == EntryType.REBUILD_MAX)
				{
					rebuildMax = l;
					break;
				}
			}
		}
	}

	/**
	 * Get the number of entries in this log.
	 *
	 * @return
	 */
	public int size()
	{
		return data.size() - 1;
	}

	/**
	 * Clear this log. Used when a full index rebuild is being made.
	 */
	public void clear()
	{
		for(Entry e : data.values())
		{
			if(e.indexDataId > 0)
			{
				onStoredIdDiscarded.accept(e.indexDataId);
			}
		}

		data.clear();
		data.put(0l, new Entry(EntryType.HARD_COMMIT, 0, 0));
	}

	/**
	 * Get the location of the rebuild pointer.
	 *
	 * @return
	 *   if of data that was last rebuilt
	 */
	public long getGenerationPointer()
	{
		Iterator<Long> it = data.keyIterator(0l);
		while(it.hasNext())
		{
			Long l = it.next();
			Entry e = data.get(l);
			if(e.getType() == EntryType.GENERATION_POINTER)
			{
				return e.dataId;
			}
		}

		return 0;
	}

	/**
	 * Set the location of the rebuild pointer.
	 *
	 * @param dataId
	 *   id of data that was last rebuilt
	 */
	public long setGenerationPointer(long dataId)
	{
		long previousIndex = -1;
		Iterator<Long> it = data.keyIterator(0l);
		while(it.hasNext())
		{
			Long l = it.next();
			Entry e = data.get(l);
			if(e.getType() == EntryType.GENERATION_POINTER)
			{
				previousIndex = l;
				break;
			}
		}

		// TODO: Optimization for previousIndex - store in local variable
		long opId = (previousIndex == -1 ? 0 : previousIndex) + 1;
		if(opId > rebuildMax)
		{
			throw new StorageException("No more operations available for rebuild");
		}

		if(previousIndex >= 0)
		{
			data.remove(previousIndex);
		}

		data.put(opId, new Entry(EntryType.GENERATION_POINTER, dataId, 0));
		return opId;
	}

	/**
	 * Remove the current rebuild pointer.
	 */
	public void removeGenerationPointer()
	{
		Iterator<Long> it = data.keyIterator(0l);
		while(it.hasNext())
		{
			Long l = it.next();
			Entry e = data.get(l);
			if(e.getType() == EntryType.GENERATION_POINTER)
			{
				data.remove(l);
				break;
			}
		}
	}

	/**
	 * Get the last hard commit that the log knows of.
	 *
	 * @return
	 */
	public long getLastHardCommit()
	{
		Iterator<Long> it = data.keyIterator(0l);
		while(it.hasNext())
		{
			Long l = it.next();
			Entry e = data.get(l);
			if(e.getType() == EntryType.HARD_COMMIT)
			{
				return l;
			}
		}

		return 0;
	}

	/**
	 * Get the last stored id that is the current operation or before the
	 * current operation.
	 *
	 * @return
	 */
	public long getLastStoredId(long currentOp)
	{
		Long op = currentOp;
		while(true)
		{
			Entry entry = data.get(op);
			if(entry == null) return 0;

			if(entry.type == EntryType.STORE || entry.type == EntryType.HARD_COMMIT)
			{
				return entry.dataId;
			}

			op = data.lowerKey(op);
		}
	}

	/**
	 * Set the last hard commit that the engine has done. This will let the
	 * log drop everything up to this point.
	 *
	 * @param id
	 */
	public void setLastHardCommit(long id)
	{
		Iterator<Long> it = data.keyIterator(0l);
		long lastStoredId = 0;
		while(it.hasNext())
		{
			Long l = it.next();
			if(l > id) break;

			Entry e = data.get(l);
			switch(e.getType())
			{
				case STORE:
					lastStoredId = e.getId();
					data.remove(l);
					onStoredIdDiscarded.accept(e.indexDataId);
					break;
				case DELETION:
				case HARD_COMMIT:
					data.remove(l);
					break;
				case REBUILD_MAX:
				case GENERATION_POINTER:
					// Do nothing - these are virtual operations
			}
		}

		data.put(id, new Entry(EntryType.HARD_COMMIT, lastStoredId, 0));
	}

	public long getLatestOp()
	{
		return data.lastKey();
	}

	/**
	 * Append a delete action to the log.
	 *
	 * @param dataId
	 *   id of data deleted
	 * @return
	 *   id representing this action
	 */
	public long appendDelete(long dataId)
	{
		long next = nextKey();
		data.put(next, new Entry(EntryType.DELETION, dataId, 0));
		return next;
	}

	/**
	 * Append a store action to the log.
	 *
	 * @param dataId
	 *   if of data stored
	 * @param indexDataId
	 *   id of data in {@link DataStorage}
	 * @return
	 *   id representing this action
	 */
	public long appendStore(long dataId, long indexDataId)
	{
		long next = nextKey();
		data.put(next, new Entry(EntryType.STORE, dataId, indexDataId));
		return next;
	}

	/**
	 * Get the largest operation id a {@link #appendRebuild(long, long)} can
	 * have.
	 *
	 * @return
	 */
	public long getRebuildMax()
	{
		return rebuildMax;
	}

	/**
	 * Get the last data id that is expected to be in the rebuild.
	 *
	 * @return
	 */
	public long getRebuildMaxDataId()
	{
		if(rebuildMax == 0) return 0;

		return data.get(rebuildMax).dataId;
	}

	/**
	 * Set the expected number of data entries being rebuilt.
	 *
	 * @param storesExpected
	 */
	public void setRebuildMax(long storesExpected, long lastDataId)
	{
		long pointer = 0;

		Iterator<Long> it = data.keyIterator(0l);
		while(it.hasNext())
		{
			Long l = it.next();
			Entry e = data.get(l);
			if(e.getType() == EntryType.GENERATION_POINTER)
			{
				pointer = l;
			}
			else if(e.getType() == EntryType.REBUILD_MAX)
			{
				data.remove(l);
				break;
			}
		}

		long opId = pointer + storesExpected;
		data.put(opId, new Entry(EntryType.REBUILD_MAX, lastDataId, 0));
		rebuildMax = opId;
	}

	/**
	 * Get the last rebuild operation stored.
	 *
	 * @return
	 */
	public long getLastRebuildOp()
	{
		Long largest = data.lowerKey(rebuildMax);
		return largest == null ? 0 : largest;
	}

	/**
	 * Get the last rebuild operation stored.
	 *
	 * @return
	 */
	public long getLastRebuildOpData()
	{
		Long largest = data.lowerKey(rebuildMax);
		return largest == null ? 0 : data.get(largest).indexDataId;
	}

	/**
	 * Append a rebuild action to the log. This is similar to
	 * {@link #appendStore(long, long)} but will append to the virtual area
	 * created via {@link #setRebuildMax(long)}.
	 *
	 * @param dataId
	 *   if of data stored
	 * @param indexDataId
	 *   id of data in {@link DataStorage}
	 * @return
	 *   id representing this action
	 */
	public long appendRebuild(long dataId, long indexDataId)
	{
		Long largest = data.lowerKey(rebuildMax);
		long next = largest == null ? 1 : largest + 1;
		data.put(next, new Entry(EntryType.STORE, dataId, indexDataId));

		if(next == rebuildMax)
		{
			// This append has ended the rebuild, reset it
			rebuildMax = 0;
		}

		return next;
	}

	public Iterator<LongObjectPair<Entry>> iterator(long lastOp)
	{
		Long firstKey = data.higherKey(lastOp);
		if(firstKey == null)
		{
			return EmptyIterator.getInstance();
		}

		Iterator<Long> it = data.keyIterator(firstKey);
		return new Iterator<LongObjectPair<Entry>>()
		{
			private LongObjectPair<Entry> next;

			@Override
			public boolean hasNext()
			{
				return next != null || findNext();
			}

			@Override
			public LongObjectPair<Entry> next()
			{
				if(next == null && ! findNext())
				{
					throw new NoSuchElementException();
				}

				LongObjectPair<Entry> result =  next;
				next = null;
				return result;
			}

			private boolean findNext()
			{
				while(it.hasNext())
				{
					Long key = it.next();
					Entry e = data.get(key);

					if(e.getType() == EntryType.STORE || e.getType() == EntryType.DELETION)
					{
						next = PrimitiveTuples.pair(key.longValue(), data.get(key));
						return true;
					}
				}

				return false;
			}
		};
	}

	private long nextKey()
	{
		Long lastKey = data.lastKey();
		return lastKey == null ? 1 : (lastKey + 1);
	}

	public static class Entry
	{
		private final EntryType type;
		private final long dataId;
		private final long indexDataId;

		public Entry(
			EntryType type,
			long dataId,
			long indexDataId
		)
		{
			this.type = type;
			this.dataId = dataId;
			this.indexDataId = indexDataId;
		}

		public EntryType getType()
		{
			return type;
		}

		public long getId()
		{
			return dataId;
		}

		public long getIndexDataId()
		{
			return indexDataId;
		}
	}

	public static enum EntryType
	{
		HARD_COMMIT,

		DELETION,

		STORE,

		GENERATION_POINTER,

		REBUILD_MAX
	}

	private static class EntryFieldType
		implements DataType
	{
		public static final DataType INSTANCE = new EntryFieldType();

		@Override
		public int getMemory(Object obj)
		{
			int result = 16;

			// Enum
			result += 24;

			// Data Id
			result += 30;

			// Index Data Id
			result += 30;

			return result;
		}

		@Override
		public Entry read(ByteBuffer in)
		{
			EntryType type;
			switch(in.get())
			{
				case 0:
					type = EntryType.HARD_COMMIT;
					break;
				case 1:
					type = EntryType.DELETION;
					break;
				case 2:
					type = EntryType.STORE;
					break;
				case 3:
					type = EntryType.REBUILD_MAX;
					break;
				case 4:
					type = EntryType.GENERATION_POINTER;
					break;
				default:
					throw new StorageException("Corrupted, unknown type");
			}

			long id = DataUtils.readVarLong(in);
			long dataId = DataUtils.readVarLong(in);

			return new Entry(type, id, dataId);
		}

		@Override
		public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
		{
			for(int i=0; i<len; i++)
			{
				obj[i] = read(buff);
			}
		}

		@Override
		public void write(WriteBuffer out, Object obj)
		{
			Entry entry = (Entry) obj;
			switch(entry.type)
			{
				case HARD_COMMIT:
					out.put((byte) 0);
					break;
				case DELETION:
					out.put((byte) 1);
					break;
				case STORE:
					out.put((byte) 2);
					break;
				case REBUILD_MAX:
					out.put((byte) 3);
					break;
				case GENERATION_POINTER:
					out.put((byte) 4);
					break;
			}

			out.putVarLong(entry.dataId);
			out.putVarLong(entry.indexDataId);
		}

		@Override
		public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
		{
			for(int i=0; i<len; i++)
			{
				write(buff, obj[i]);
			}
		}

		@Override
		public int compare(Object o1, Object o2)
		{
			Entry e1 = (Entry) o1;
			Entry e2 = (Entry) o2;

			int c = Integer.compare(e1.type.ordinal(), e2.type.ordinal());
			if(c != 0) return c;

			c = Long.compare(e1.dataId, e2.dataId);
			if(c != 0) return c;

			return Long.compare(e1.indexDataId, e2.indexDataId);
		}
	}
}
