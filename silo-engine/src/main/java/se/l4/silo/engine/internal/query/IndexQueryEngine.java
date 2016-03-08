package se.l4.silo.engine.internal.query;

import java.io.IOException;

import se.l4.silo.engine.DataEncounter;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.config.IndexConfig;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.index.IndexQueryRequest;

public class IndexQueryEngine
	implements QueryEngine<IndexQueryRequest>
{
	private final MVStoreManager store;

	public IndexQueryEngine(String name, MVStoreManager store, IndexConfig config)
	{
		this.store = store;
		store.openMap("data:" + name, LongFieldType.INSTANCE, LongFieldType.INSTANCE);
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
	}

	@Override
	public void delete(long id)
	{
		System.out.println("Delete " + id);
	}

}
