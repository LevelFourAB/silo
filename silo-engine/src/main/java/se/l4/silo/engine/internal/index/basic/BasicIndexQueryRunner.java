package se.l4.silo.engine.internal.index.basic;

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
import org.eclipse.collections.api.list.MutableList;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.DataType;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.index.basic.BasicFieldDef;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.MaxMin;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.FieldSort;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.basic.BasicIndexQuery;
import se.l4.silo.index.basic.BasicIndexResult;

public class BasicIndexQueryRunner<T>
	implements IndexQueryRunner<T, BasicIndexQuery<T>>
{
	private final Logger logger;

	private final BasicFieldDef<T, ?>[] fields;
	private final BasicFieldDef.Single<T, ?>[] sortFields;

	private final TransactionValue<ReadOnlyIndex> indexMapValue;

	public BasicIndexQueryRunner(
		Logger logger,

		MVStoreManager store,

		BasicFieldDef<T, ?>[] fields,
		BasicFieldDef.Single<T, ?>[] sortFields,

		MVMap<Object[], Object[]> index
	)
	{
		this.logger = logger;

		this.fields = fields;
		this.sortFields = sortFields;

		indexMapValue = v -> {
			MVStoreManager.VersionHandle handle = store.acquireVersionHandle();
			return new ReadOnlyIndex(handle, index.openVersion(handle.getVersion()));
		};
	}

	@Override
	public void provideTransactionValues(
		Consumer<? super TransactionValue<?>> consumer
	)
	{
		consumer.accept(indexMapValue);
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

	private BasicIndexResult<T> runQuery(IndexQueryEncounter<? extends BasicIndexQuery<T>, T> encounter)
	{
		BasicIndexQuery<T> query = encounter.getQuery();
		logger.debug("Query {}", query);

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
			if(sortFields[i].getName().equals(name))
			{
				return i;
			}
		}

		throw new StorageException("The field `" + name + "` does not exist in this index");
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

	public abstract class ResultCollector
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
