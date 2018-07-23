package se.l4.silo.engine.internal.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongFunction;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.query.QueryFetchResult;
import se.l4.silo.query.QueryResult;
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
	private final Map<String, Object> metadata;

	private long offset;
	private long limit;
	private long totalHits;

	public QueryEncounterImpl(T data, LongFunction<R> dataLoader)
	{
		this.data = data;
		this.dataLoader = dataLoader;
		this.result = new ArrayList<>();
		this.metadata = new HashMap<>();
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
		result.add(new QueryResultImpl<>(id, Collections.emptyMap(), dataLoader));
	}

	@Override
	public void receive(long id, Consumer<BiConsumer<String, Object>> metadataCreator)
	{
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		metadataCreator.accept(builder::put);

		result.add(new QueryResultImpl<>(id, builder.build(), dataLoader));
	}

	@Override
	public void addMetadata(String key, Object value)
	{
		metadata.put(key, value);
	}

	@Override
	public void setMetadata(long offset, long limit, long totalHits)
	{
		this.offset = offset;
		this.limit = limit;
		this.totalHits = totalHits;
	}

	public QueryFetchResult<QueryResult<R>> getResult()
	{
		IteratorFetchResult<QueryResult<R>> fr = new IteratorFetchResult<>(result, offset, limit, totalHits);
		return new DelegatingQueryFetchResult<>(fr, metadata);
	}

	private static class QueryResultImpl<R>
		implements QueryResult<R>
	{
		private final long id;
		private final LongFunction<R> loader;
		private final Map<String, Object> metadata;

		public QueryResultImpl(long id, Map<String, Object> metadata, LongFunction<R> loader)
		{
			this.id = id;
			this.metadata = metadata;
			this.loader = loader;
		}

		@Override
		public R getData()
		{
			return loader.apply(id);
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public <M> M getMetadata(String key)
		{
			return (M) metadata.get(key);
		}

	}
}
