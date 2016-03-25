package se.l4.silo.engine.internal.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.type.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.StorageException;
import se.l4.silo.engine.DataEncounter;
import se.l4.silo.engine.FieldDef;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.config.IndexConfig;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.MaxMin;
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.index.IndexQueryRequest;
import se.l4.silo.index.IndexQueryRequest.Criterion;
import se.l4.silo.index.IndexQueryRequest.SortOnField;

/**
 * {@link QueryEngine} that creates a queryable index similiar in functionality
 * to traditional databases.
 * 
 * @author Andreas Holstenson
 *
 */
public class IndexQueryEngine
	implements QueryEngine<IndexQueryRequest>
{
	private static final Logger logger = LoggerFactory.getLogger(IndexQueryEngine.class);
	
	private final String name;
	
	private final MVStoreManager store;
	private final MVMap<Long, Object[]> indexedData;
	private final MVMap<Object[], Object[]> index;
	
	private final String[] fields;
	private final FieldType[] fieldTypes;
	private final String[] sortFields;
	private final FieldType[] sortFieldTypes;

	public IndexQueryEngine(String name, Fields fields, MVStoreManager store, IndexConfig config)
	{
		this.name = name;
		this.store = store;
		
		this.fields = config.getFields();
		this.sortFields = config.getSortFields();
		
		MergedFieldType indexKey = createFieldType(name, fields, config.getFields(), true);
		this.fieldTypes = indexKey.getTypes();
		indexedData = store.openMap("data:" + name, LongFieldType.INSTANCE, indexKey);
		
		MergedFieldType indexData = createFieldType(name, fields, config.getSortFields(), false);
		this.sortFieldTypes = indexData.getTypes();
		index = store.openMap("index:" + name, indexKey, indexData);
	}
	
	/**
	 * Create a {@link MergedFieldType} for the given fields.
	 * 
	 * @param name
	 * @param fields
	 * @param fieldNames
	 * @param appendId
	 * @return
	 */
	private MergedFieldType createFieldType(String name, Fields fields, String[] fieldNames, boolean appendId)
	{
		FieldType[] result = new FieldType[fieldNames.length + (appendId ? 1 : 0)];
		for(int i=0, n=fieldNames.length; i<n; i++)
		{
			String field = fieldNames[i];
			Optional<FieldDef> def = fields.get(field);
			if(! def.isPresent())
			{
				throw new StorageException("Problem creating index query engine `" + name + "`, trying to use unknown field `" + field + "`");
			}
			result[i] = def.get().getType();
		}
		
		if(appendId)
		{
			result[fieldNames.length] = LongFieldType.INSTANCE;
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
	public void update(long id, DataEncounter encounter)
	{
		logger.debug("{}: Update entry for id {}", name, id);
		
		// Generate the key for the map
		Object[] key = encounter.getStructuredArray(fields, 1);
		key[key.length - 1] = id;
		
		// Generate the sort data
		Object[] data = encounter.getStructuredArray(sortFields);
		
		// Look up our previously indexed key to see if we need to delete it
		Object[] previousKey = indexedData.get(id);
		if(previousKey != null)
		{
			index.remove(previousKey);
		}
		
		// Store the new key
		index.put(key, data);
	}

	@Override
	public void delete(long id)
	{
		logger.debug("{}: Delete entry for id {}", name, id);
		Object[] previousKey = indexedData.get(id);
		if(previousKey != null)
		{
			index.remove(previousKey);
		}
	}
	
	private int findField(String name, boolean errorOnNoFind)
	{
		for(int i=0, n=fields.length; i<n; i++)
		{
			if(fields[i].equals(name))
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

	@Override
	public void query(QueryEncounter<IndexQueryRequest> encounter)
	{
		IndexQueryRequest request = encounter.getData();
		logger.debug("{}: Perform query {}", name, request);
		
		QueryPart[] parts = new QueryPart[fields.length + 1];
		for(Criterion c : request.getCriterias())
		{
			int field = findField(c.getField(), true);
			
			FieldType<?> ft = fieldTypes[field];
			
			switch(c.getOp())
			{
				case EQUAL:
					parts[field] = new EqualsQuery(field, c.getValue());
					break;
				case MORE_THAN: 
				{
					QueryPart qp = parts[field];
					if(qp instanceof RangeQuery)
					{
						qp = new RangeQuery(field, increase(ft, c.getValue()), ((RangeQuery) qp).upper);
					}
					else
					{
						qp = new RangeQuery(field, increase(ft, c.getValue()), MaxMin.MAX);
					}
					
					parts[field] = qp;
					break;
				}
				case MORE_THAN_OR_EQUAL_TO:
				{
					QueryPart qp = parts[field];
					if(qp instanceof RangeQuery)
					{
						qp = new RangeQuery(field, convert(ft, c.getValue()), ((RangeQuery) qp).upper);
					}
					else
					{
						qp = new RangeQuery(field, convert(ft, c.getValue()), MaxMin.MAX);
					}
					
					parts[field] = qp;
					break;
				}
				case LESS_THAN:
				{
					QueryPart qp = parts[field];
					if(qp instanceof RangeQuery)
					{
						qp = new RangeQuery(field, ((RangeQuery) qp).lower, decrease(ft, c.getValue()));
					}
					else
					{
						qp = new RangeQuery(field, MaxMin.MIN, decrease(ft, c.getValue()));
					}
					
					parts[field] = qp;
					break;
				}
				case LESS_THAN_OR_EQUAL_TO:
				{
					QueryPart qp = parts[field];
					if(qp instanceof RangeQuery)
					{
						qp = new RangeQuery(field, ((RangeQuery) qp).lower, convert(ft, c.getValue()));
					}
					else
					{
						qp = new RangeQuery(field, MaxMin.MIN, convert(ft, c.getValue()));
					}
					
					parts[field] = qp;
					break;
				}
			}
		}
		
		// Setup sorting of the results
		boolean hasSort = false;
		Sort.Builder sortBuilder = Sort.builder();
		for(SortOnField s : request.getSort())
		{
			int field = findField(s.getField(), false);
			if(field >= 0)
			{
				sortBuilder.key(field, fieldTypes[field], s.isAscending());
			}
			else
			{
				hasSort = true;
				field = findSortField(s.getField());
				sortBuilder.value(field, sortFieldTypes[field], s.isAscending());
			}
		}
		
		Comparator<Result> sort = sortBuilder
			.id(true)
			.build();
		
		// Fetch the limits and setup collection of results
		int limit = request.getLimit();
		int offset = request.getOffset();
		
		
		LongAdder total = new LongAdder();
		int maxSize = limit > 0 ? offset + limit : 0;
		TreeSet<Result> tree = new TreeSet<>(sort);
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
		
		// Cut down the results if needed
		if(maxSize != 0 && tree.size() > limit)
		{
			int n = tree.size() - offset;
			Iterator<Result> it = tree.descendingIterator();
			for(int i=n-1; i>=0; i--)
			{
				Result result = it.next();
				encounter.receive(result.getId());
			}
		}
		else
		{
			for(Result r : tree)
			{
				encounter.receive(r.getId());
			}
		}
		
		encounter.setMetadata(offset, limit, total.intValue());
	}
	
	private Number convert(FieldType<?> type, Object v)
	{
		return (Number) type.convert(v);
	}
	
	private Object increase(FieldType<?> type, Object v)
	{
		Number n = (Number) type.convert(v);
		if(n instanceof Double)
		{
			return n.doubleValue() + 0.0000001;
		}
		else if(n instanceof Float)
		{
			return n.byteValue() + 0.0000001;
		}
		else if(n instanceof Integer)
		{
			return n.intValue() + 1;
		}
		else if(n instanceof Long)
		{
			return n.longValue() + 1;
		}
		
		throw new IllegalArgumentException("Can not increase value of " + v);
	}
	
	private Object decrease(FieldType<?> type, Object v)
	{
		Number n = (Number) type.convert(v);
		if(n instanceof Double)
		{
			return n.doubleValue() - 0.0000001;
		}
		else if(n instanceof Float)
		{
			return n.byteValue() - 0.0000001;
		}
		else if(n instanceof Integer)
		{
			return n.intValue() - 1;
		}
		else if(n instanceof Long)
		{
			return n.longValue() - 1;
		}
		
		throw new IllegalArgumentException("Can not decrease value of " + v);
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
				for(Object o : ((Iterable) value))
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
		public void run(Object[] lower,
				Object[] upper,
				QueryPart[] parts,
				Predicate<Object[]> filter)
		{
			lower[id] = MaxMin.MIN;
			upper[id] = MaxMin.MAX;
			
			DataType keyType = map.getKeyType();
			
			if(logger.isTraceEnabled())
			{
				logger.trace("  Query: lower= " + Arrays.toString(lower) + " " + Arrays.toString(types(lower)));
				logger.trace("  Query: upper= " + Arrays.toString(upper) + " " + Arrays.toString(types(upper)));
				logger.trace("  Query: lower <> upper = " + keyType.compare(lower, upper));
			}
			
			Object[] previous = map.lowerKey(lower);
			if(previous == null)
			{
				previous = map.firstKey();
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
}
