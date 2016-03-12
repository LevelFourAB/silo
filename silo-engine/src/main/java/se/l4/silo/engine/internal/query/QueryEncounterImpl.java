package se.l4.silo.engine.internal.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

import se.l4.silo.FetchResult;
import se.l4.silo.QueryResult;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.results.IteratorFetchResult;

/**
 * Implementation of {@link QueryEncounter}.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class QueryEncounterImpl<T, R>
	implements QueryEncounter<T>
{
	private final T data;
	private final LongFunction<R> dataLoader;
	private final List<QueryResult<R>> result;
	private int offset;
	private int limit;
	private int totalHits;

	public QueryEncounterImpl(T data, LongFunction<R> dataLoader)
	{
		this.data = data;
		this.dataLoader = dataLoader;
		this.result = new ArrayList<>();
	}

	@Override
	public T getData()
	{
		return data;
	}
	
	@Override
	public Object load(long id)
	{
		return dataLoader.apply(id);
	}
	
	@Override
	public void receive(long id)
	{
		result.add(new QueryResultImpl<>(id, dataLoader));
	}
	
	@Override
	public void setMetadata(int offset, int limit, int totalHits)
	{
		this.offset = offset;
		this.limit = limit;
		this.totalHits = totalHits;
	}
	
	public FetchResult<QueryResult<R>> getResult()
	{
		return new IteratorFetchResult<>(result, offset, limit, totalHits);
	}
	
	private static class QueryResultImpl<R>
		implements QueryResult<R>
	{
		private final long id;
		private final LongFunction<R> loader;

		public QueryResultImpl(long id, LongFunction<R> loader)
		{
			this.id = id;
			this.loader = loader;
		}

		@Override
		public R getData()
		{
			return loader.apply(id);
		}

		@Override
		public Object getMetadata(String key)
		{
			return null;
		}
		
	}
}
