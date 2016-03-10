package se.l4.silo.engine.internal.query;

import java.io.IOException;
import java.util.Optional;

import org.h2.mvstore.MVMap;

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
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.index.IndexQueryRequest;

public class IndexQueryEngine
	implements QueryEngine<IndexQueryRequest>
{
	private final MVStoreManager store;
	private final MVMap<Long, Object[]> indexedData;
	private final MVMap<Object[], Object[]> index;
	
	private final String[] fields;
	private final String[] sortFields;

	public IndexQueryEngine(String name, Fields fields, MVStoreManager store, IndexConfig config)
	{
		this.store = store;
		
		this.fields = config.getFields();
		this.sortFields = config.getSortFields();
		
		MergedFieldType indexKey = createFieldType(name, fields, config.getFields(), true);
		indexedData = store.openMap("data:" + name, LongFieldType.INSTANCE, indexKey);
		
		MergedFieldType indexData = createFieldType(name, fields, config.getSortFields(), false);
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
	public void query(QueryEncounter<IndexQueryRequest> encounter)
	{
		IndexQueryRequest request = encounter.getData();
		System.out.println("Query on " + request);
	}

	@Override
	public void update(long id, DataEncounter encounter)
	{
		System.out.println("Update " + id);
		
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
		System.out.println("Delete " + id);
		Object[] previousKey = indexedData.get(id);
		if(previousKey != null)
		{
			index.remove(previousKey);
		}
	}

}
