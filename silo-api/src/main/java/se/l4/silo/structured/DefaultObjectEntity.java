package se.l4.silo.structured;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

import se.l4.commons.serialization.Serializer;
import se.l4.commons.serialization.format.BinaryInput;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryResult;
import se.l4.silo.query.QueryRunner;
import se.l4.silo.query.QueryType;

/**
 * Implementation of {@link ObjectEntity}.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class DefaultObjectEntity<T>
	implements ObjectEntity<T>
{
	private final StructuredEntity parent;
	private final Serializer<T> serializer;

	public DefaultObjectEntity(StructuredEntity parent, Serializer<T> serializer)
	{
		this.parent = parent;
		this.serializer = serializer;
	}

	@Override
	public String getName()
	{
		return parent.getName();
	}

	@Override
	public T get(Object id)
	{
		try(FetchResult<StreamingInput> fr = parent.get(id))
		{
			Iterator<StreamingInput> it = fr.iterator();
			if(! it.hasNext())
			{
				return null;
			}
			
			try(StreamingInput in = it.next())
			{
				return serializer.read(in);
			}
			catch(IOException e)
			{
				throw new StorageException("Unable to read object; " + e.getMessage());
			}
		}
	}

	@Override
	public void delete(Object id)
	{
		parent.delete(id);
	}

	@Override
	public void store(Object id, T data)
	{
		try
		{
			byte[] binary = serializer.toBytes(data);
			parent.store(id, new BinaryInput(new ByteArrayInputStream(binary)));
		}
		catch(Exception e)
		{
			throw new StorageException("Unable to store object; " + e.getMessage());
		}
	}

	@Override
	public <RT, Q extends Query<?>> Q query(String engine, QueryType<T, RT, Q> type)
	{
		return parent.query(engine, new TransformingQueryType<StreamingInput, T, RT, Q>(type, in -> {
			try
			{
				return serializer.read(in);
			}
			catch(IOException e)
			{
				throw new StorageException("Unable to read object; " + e.getMessage());
			}
			finally
			{
				try
				{
					in.close();
				}
				catch(Exception e)
				{
				}
			}
		}));
	}

	private static class TransformingQueryType<TransformedFrom, EntityDataType, ResultType, Q extends Query<?>>
		implements QueryType<TransformedFrom, ResultType, Q>
	{
		private QueryType<EntityDataType, ResultType, Q> qt;
		private Function<TransformedFrom, EntityDataType> translator;

		public TransformingQueryType(QueryType<?, ?, Q> qt, Function<TransformedFrom, EntityDataType> translator)
		{
			this.qt = (QueryType<EntityDataType, ResultType, Q>) qt;
			this.translator = translator;
		}
		
		@Override
		public Q create(QueryRunner<TransformedFrom, ResultType> runner)
		{
			return qt.create((data, translator) -> {
				return runner.fetchResults(data, in -> {
					return translator.apply(new TransformedQueryResult<>(in, this.translator));
				});
			});
		}
	}
	
	private static class TransformedQueryResult<From, To>
		implements QueryResult<To>
	{
		private final QueryResult<From> other;
		private final Function<From, To> translator;

		public TransformedQueryResult(QueryResult<From> other, Function<From, To> translator)
		{
			this.other = other;
			this.translator = translator;
		}
		
		@Override
		public To getData()
		{
			return translator.apply(other.getData());
		}
		
		@Override
		public Object getMetadata(String key)
		{
			return other.getMetadata(key);
		}
	}
}
