package se.l4.silo.engine.internal.index.basic;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.index.IndexEngine;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.engine.types.ArrayFieldType;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.MaxMin;
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.engine.types.StringFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.FieldSort;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.basic.BasicIndexQuery;
import se.l4.silo.index.basic.BasicIndexResult;

/**
 * {@link IndexEngine} that creates a queryable index similar in functionality
 * to traditional databases.
 */
public class BasicIndexEngine<T>
	implements IndexEngine<T, BasicIndexQuery<T>>
{
	private static final Logger logger = LoggerFactory.getLogger(BasicIndexEngine.class);

	private final String name;
	private final String uniqueName;

	private final MVStoreManager store;
	private final MVMap<String, Long> indexState;
	private final MVMap<Long, Object[]> indexedData;
	private final MVMap<Object[], Object[]> index;
	private final TransactionValue<ReadOnlyIndex> indexMapValue;

	private final BasicFieldDefinition<T>[] fields;
	private final BasicFieldDefinition<T>[] sortFields;

	private final MergedFieldType dataFieldType;
	private final MergedFieldType indexKey;
	private final MergedFieldType indexData;

	private volatile long hardCommit;

	public BasicIndexEngine(
		MVStoreManager store,
		String name,
		String uniqueName,
		ListIterable<BasicFieldDefinition<T>> fields,
		ListIterable<BasicFieldDefinition<T>> sortFields
	)
	{
		this.name = name;
		this.uniqueName = uniqueName;
		this.store = store;

		this.fields = fields.toArray(new BasicFieldDefinition[fields.size()]);
		this.sortFields = sortFields.toArray(new BasicFieldDefinition[sortFields.size()]);

		this.dataFieldType = createMultiFieldType(fields, false);
		indexedData = store.openMap("data:" + uniqueName, LongFieldType.INSTANCE, dataFieldType);

		this.indexKey = createFieldType(fields, true);
		this.indexData = createFieldType(sortFields, false);
		index = store.openMap("index:" + uniqueName, indexKey, indexData);

		indexState = store.openMap("state", StringFieldType.INSTANCE, LongFieldType.INSTANCE);

		if(logger.isDebugEnabled())
		{
			logger.debug(uniqueName + ": fields=" + Arrays.toString(this.fields) + ", sortFields=" + Arrays.toString(this.sortFields));
		}

		indexMapValue = v -> {
			MVStoreManager.VersionHandle handle = store.acquireVersionHandle();
			return new ReadOnlyIndex(handle, index.openVersion(handle.getVersion()));
		};

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
	public String getName()
	{
		return name;
	}

	@Override
	public void provideTransactionValues(
		Consumer<? super TransactionValue<?>> consumer
	)
	{
		consumer.accept(indexMapValue);
	}

	@Override
	public long getLastHardCommit()
	{
		return hardCommit;
	}

	/**
	 * Create a {@link MergedFieldType} for the given fields.
	 *
	 * @param uniqueName
	 * @param fields
	 * @param fieldNames
	 * @param appendId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static <T> MergedFieldType createFieldType(
		ListIterable<BasicFieldDefinition<T>> fields,
		boolean appendId
	)
	{
		FieldType[] result = new FieldType[fields.size() + (appendId ? 1 : 0)];
		for(int i=0, n=fields.size(); i<n; i++)
		{
			BasicFieldDefinition field = fields.get(i);
			result[i] = field.getType();
		}

		if(appendId)
		{
			result[fields.size()] = LongFieldType.INSTANCE;
		}

		return new MergedFieldType(result);
	}

	/**
	 * Create a {@link MergedFieldType} that allows multiple values for
	 * every field.
	 *
	 * @param uniqueName
	 * @param fields
	 * @param fieldNames
	 * @param appendId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static <T> MergedFieldType createMultiFieldType(
		ListIterable<BasicFieldDefinition<T>> fields,
		boolean appendId
	)
	{
		FieldType[] result = new FieldType[fields.size() + (appendId ? 1 : 0)];
		for(int i=0, n=fields.size(); i<n; i++)
		{
			BasicFieldDefinition field = fields.get(i);
			result[i] = new ArrayFieldType(field.getType());
		}

		if(appendId)
		{
			result[fields.size()] = LongFieldType.INSTANCE;
		}

		return new MergedFieldType(result);
	}

	@Override
	public void close()
		throws IOException
	{
		store.close();
	}

	@Override
	public void clear()
	{
		indexedData.clear();
		index.clear();
		indexState.put(uniqueName, 0l);
		hardCommit = 0;
	}

	@Override
	public void generate(T data, ExtendedDataOutputStream out)
		throws IOException
	{
		// Write a version tag
		out.write(0);

		Object[][] key = new Object[fields.length][];
		for(int i=0, n=key.length; i<n; i++)
		{
			Object o = fields[i].getSupplier().apply(data);
			key[i] = o instanceof Iterable
				? Lists.immutable.ofAll((Iterable) o).toArray()
				: new Object[] { o };
		}

		Object[] sort = new Object[sortFields.length];
		for(int i=0, n=sortFields.length; i<n; i++)
		{
			sort[i] = sortFields[i].getSupplier().apply(data);
		}

		dataFieldType.write(key, out);
		indexData.write(sort, out);
	}

	@Override
	public void apply(long op, long id, ExtendedDataInputStream in)
		throws IOException
	{
		int version = in.read();
		if(version != 0)
		{
			throw new StorageException("Unknown field index version encountered: " + version);
		}

		logger.debug("{}: Update entry for id {}", uniqueName, id);

		Object[] keyData = dataFieldType.read(in);
		Object[] sort = indexData.read(in);

		// Look up our previously indexed key to see if we need to delete it
		Object[] previousKey = indexedData.get(id);
		remove(previousKey, id);

		// Store the new key
		store(keyData, sort, id);

		// Store that we have applied this operation
		indexState.put(uniqueName, op);
	}

	private void store(Object[] keyData, Object[] sortData, long id)
	{
		Object[] generatedKey = new Object[fields.length + 1];
		generatedKey[fields.length] = id;

		if(fields.length == 0)
		{
			// Special case for only sort data
			if(logger.isTraceEnabled())
			{
				logger.trace("  storing key=" + Arrays.toString(generatedKey) + ", sort=" + Arrays.toString(sortData));
			}
			index.put(generatedKey, sortData);
		}
		else
		{
			recursiveStore(keyData, sortData, generatedKey, 0);
		}

		// Store a combined key for indexedData
		indexedData.put(id, keyData);
	}

	private void recursiveStore(
		Object[] keyData,
		Object[] sortData,
		Object[] generatedKey,
		int i
	)
	{
		if(i == fields.length - 1)
		{
			for(Object o : (Object[]) keyData[i])
			{
				generatedKey[i] = o;
				if(logger.isTraceEnabled())
				{
					logger.trace("  storing key=" + Arrays.toString(generatedKey) + ", sort=" + Arrays.toString(sortData));
				}
				Object[] keyCopy = Arrays.copyOf(generatedKey, generatedKey.length);
				index.put(keyCopy, sortData);
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

	private void remove(Object[] data, long id)
	{
		if(data == null) return;

		Object[] generatedKey = new Object[data.length + 1];
		generatedKey[data.length] = id;

		if(fields.length == 0)
		{
			index.remove(generatedKey);
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
				index.remove(generatedKey);
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

	@Override
	public void delete(long op, long id)
	{
		logger.debug("{}: Delete entry for id {}", uniqueName, id);
		Object[] previousKey = indexedData.get(id);
		remove(previousKey, id);
		indexedData.remove(id);

		// Store that we have applied this operation
		indexState.put(uniqueName, op);
	}

	private int findField(String name, boolean errorOnNoFind)
	{
		for(int i=0, n=fields.length; i<n; i++)
		{
			if(fields[i].getName().equals(name))
			{
				return i;
			}
		}

		if(errorOnNoFind)
		{
			throw new StorageException("The field `" + name + "` does not exist in this index");
		}

		return -1;
	}

	private int findSortField(String name)
	{
		for(int i=0, n=sortFields.length; i<n; i++)
		{
			if(sortFields[i].equals(name))
			{
				return i;
			}
		}

		throw new StorageException("The field `" + name + "` does not exist in this index");
	}

	private BasicIndexResult<T> runQuery(IndexQueryEncounter<? extends BasicIndexQuery<T>, T> encounter)
	{
		BasicIndexQuery<T> query = encounter.getQuery();
		logger.debug("{}: Perform query {}", uniqueName, query);

		QueryPart[] parts = new QueryPart[fields.length + 1];
		for(FieldLimit c : query.getLimits())
		{
			int field = findField(c.getField(), true);

			FieldType ft = fields[field].getType();

			Matcher matcher = c.getMatcher();
			if(matcher instanceof EqualsMatcher)
			{
				parts[field] = new EqualsQuery(field, ((EqualsMatcher) matcher).getValue());
			}
			else if(matcher instanceof RangeMatcher)
			{
				RangeMatcher<?> rangeMatcher = (RangeMatcher<?>) matcher;

				Object lower = ! rangeMatcher.getLower().isPresent() ? MaxMin.MIN
					: rangeMatcher.isLowerInclusive()
						? ft.convert(rangeMatcher.getLower().get())
						: ft.nextUp(ft.convert(rangeMatcher.getLower().get()));

				Object upper = ! rangeMatcher.getUpper().isPresent() ? MaxMin.MAX
					: rangeMatcher.isUpperInclusive()
						? ft.convert(rangeMatcher.getUpper().get())
						: ft.nextDown(ft.convert(rangeMatcher.getUpper().get()));

				parts[field] = new RangeQuery(field, lower, upper);
			}
			else
			{
				throw new StorageException("The given matcher is not supported, only basic matchers can be used with field index. Received: " + c.getMatcher());
			}
		}

		// Setup sorting of the results
		boolean hasSort = false;
		Sort.Builder sortBuilder = Sort.builder();
		for(FieldSort s : query.getSortOrder())
		{
			int field = findField(s.getField(), false);
			if(field >= 0)
			{
				sortBuilder.key(field, fields[field].getType(), s.isAscending());
			}
			else
			{
				hasSort = true;
				field = findSortField(s.getField());
				sortBuilder.value(field, sortFields[field].getType(), s.isAscending());
			}
		}

		Comparator<Result> sort = sortBuilder
			.id(query.isAscendingDefaultSort())
			.build();

		// Fetch the limits and setup collection of results
		long limit = query.getResultLimit().orElse(0);
		long offset = query.getResultOffset().orElse(0);

		LongAdder total = new LongAdder();
		long maxSize = limit > 0 ? offset + limit : 0;
		TreeSet<Result> tree = new TreeSet<>(sort);

		MVMap<Object[], Object[]> index = encounter.get(indexMapValue).indexMap;
		parts[fields.length] = new ResultCollector(index, fields.length, hasSort)
		{
			@Override
			protected boolean accept(long id, Object[] key, Object[] values)
			{
				logger.trace("  Query matched {}", id);

				total.increment();

				Result result = new Result(id, key, values);
				if(maxSize <= 0 || tree.size() < maxSize)
				{
					tree.add(result);
				}
				else if(sort.compare(tree.last(), result) > 0)
				{
					tree.pollLast();
					tree.add(result);
				}

				// TODO: Can we short circuit the result collection somehow?
				return true;
			}
		};

		// Run the collection
		Object[] lower = new Object[parts.length];
		Object[] upper = new Object[parts.length];
		parts[0].run(lower, upper, parts, (v) -> true);

		MutableList<T> items = Lists.mutable.empty();

		// Cut down the results if needed
		if(maxSize != 0 && tree.size() > limit)
		{
			long n = tree.size() - offset;
			Iterator<Result> it = tree.descendingIterator();
			for(long i=n-1; i>=0; i--)
			{
				Result result = it.next();
				items.add(encounter.load(result.getId()));
			}
		}
		else
		{
			for(Result r : tree)
			{
				items.add(encounter.load(r.getId()));
			}
		}

		return new BasicIndexResultImpl<>(items, total.sum(), offset, limit);
	}

	@Override
	public Mono<? extends FetchResult<?>> fetch(
		IndexQueryEncounter<? extends BasicIndexQuery<T>, T> encounter
	)
	{
		return Mono.fromSupplier(() -> runQuery(encounter));
	}

	@Override
	public Flux<?> stream(
		IndexQueryEncounter<? extends BasicIndexQuery<T>, T> encounter
	)
	{
		return fetch(encounter).flatMapMany(FetchResult::stream);
	}

	public interface QueryPart
	{
		void run(Object[] lower, Object[] upper, QueryPart[] parts, Predicate<Object[]> filter);
	}

	public static class RangeQuery
		implements QueryPart
	{
		private final int field;
		private final Object lower;
		private final Object upper;

		public RangeQuery(int field, Object lower, Object upper)
		{
			this.field = field;
			this.lower = lower;
			this.upper = upper;
		}

		@Override
		public void run(Object[] lower, Object[] upper, QueryPart[] parts,
				Predicate<Object[]> filter)
		{
			// Update our bounds
			lower[field] = this.lower;
			upper[field] = this.upper;

			// TODO: Do we need to do filtering here?

			// And then run the next part
			parts[field+1].run(lower, upper, parts, filter);
		}
	}

	public static class EqualsQuery
		implements QueryPart
	{
		private final int field;
		private final Object value;

		public EqualsQuery(int field, Object value)
		{
			this.field = field;
			this.value = value;
		}

		@Override
		public void run(Object[] lower, Object[] upper, QueryPart[] parts, Predicate<Object[]> filter)
		{
			if(value instanceof Iterable)
			{
				@SuppressWarnings("rawtypes")
				Iterable it = (Iterable) value;

				for(Object o : it)
				{
					// Set our bounds to this exact value
					lower[field] = o;
					upper[field] = o;

					// Extend the filter
					Predicate<Object[]> f2 = o instanceof byte[]
						? filter.and(data -> Arrays.equals((byte[]) o, (byte[]) data[field]))
						: filter.and(data -> Objects.equals(o, data[field]));

					// The run the next part
					parts[field+1].run(lower, upper, parts, f2);
				}
			}
			else
			{
				// Set our bounds to this exact value
				lower[field] = value;
				upper[field] = value;

				// Extend the filter
				filter = value instanceof byte[]
					? filter.and(data -> Arrays.equals((byte[]) value, (byte[]) data[field]))
					: filter.and(data -> Objects.equals(value, data[field]));

				// The run the next part
				parts[field+1].run(lower, upper, parts, filter);
			}
		}
	}

	public static class OrQuery
		implements QueryPart
	{
		private final int field;
		private final Collection<? extends Object> values;

		public OrQuery(int field, Collection<? extends Object> values)
		{
			this.field = field;
			this.values = values;
		}

		@Override
		public void run(Object[] lower, Object[] upper, QueryPart[] parts, Predicate<Object[]> filter)
		{
			for(Object value : values)
			{
				lower[field] = value;
				upper[field] = value;

				Predicate<Object[]> newFilter = filter.and((data) -> Objects.equals(value, data[field]));

				parts[field+1].run(lower, upper, parts, newFilter);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static Class[] types(Object[] in)
	{
		Class[] result = new Class[in.length];
		for(int i=0, n=in.length; i<n; i++)
		{
			result[i] = in[i] == null ? null : in[i].getClass();
		}

		return result;
	}

	public static abstract class ResultCollector
		implements QueryPart
	{
		private final MVMap<Object[], Object[]> map;
		private final int id;
		private final boolean sort;

		public ResultCollector(MVMap<Object[], Object[]> map, int id, boolean sort)
		{
			this.map = map;
			this.id = id;
			this.sort = sort;
		}

		@Override
		public void run(
			Object[] lower,
			Object[] upper,
			QueryPart[] parts,
			Predicate<Object[]> filter
		)
		{
			lower[id] = MaxMin.MIN;
			upper[id] = MaxMin.MAX;

			DataType keyType = map.getKeyType();

			if(logger.isTraceEnabled())
			{
				logger.trace("  Query: lower= " + Arrays.toString(lower) + " " + Arrays.toString(types(lower)));
				logger.trace("  Query: upper= " + Arrays.toString(upper) + " " + Arrays.toString(types(upper)));
				logger.trace("  Query: lower <> upper = " + keyType.compare(lower, upper));
				logger.trace("  Map size: " + map.size());
			}

			Object[] previous = map.lowerKey(lower);
			if(previous == null)
			{
				previous = map.firstKey();
				logger.trace("  First key updated to {}", previous);
			}

			if(previous == null)
			{
				// No keys
				return;
			}

			if(logger.isTraceEnabled())
			{
				logger.trace("  Query: previous= " + Arrays.toString(previous) + " " + Arrays.toString(types(previous)));
				logger.trace("  Query: previous <> lower = " + keyType.compare(previous, lower));
			}

			Iterator<Object[]> it = map.keyIterator(previous);

			while(it.hasNext())
			{
				Object[] key = it.next();

				if(logger.isTraceEnabled())
				{
					logger.trace("  Query: Got a key! " + Arrays.toString(key) + ", bound=" + keyType.compare(key, upper));
				}

				// Check the lower bound
				if(keyType.compare(key, lower) < 0) continue;

				// Check that we are within the bounds of upper
				if(keyType.compare(key, upper) > 0) return;

				if(filter.test(key))
				{
					if(! accept((Long) key[id], key, sort ? map.get(key) : null))
					{
						// Accept returned false, stop execution
						return;
					}
				}
			}
		}

		/**
		 * Accept the given id with the given sorting data.
		 *
		 * @param id
		 * @param sortData
		 * @return
		 * 		false if no more results are needed
		 */
		protected abstract boolean accept(long id, Object[] keys, Object[] values);
	}

	private static class ReadOnlyIndex
		implements TransactionValue.Releasable
	{
		private final MVStoreManager.VersionHandle versionHandle;
		private final MVMap<Object[], Object[]> indexMap;

		public ReadOnlyIndex(
			MVStoreManager.VersionHandle versionHandle,
			MVMap<Object[], Object[]> indexMap
		)
		{
			this.versionHandle = versionHandle;
			this.indexMap = indexMap;
		}

		@Override
		public void release()
		{
			versionHandle.release();
		}
	}
}
