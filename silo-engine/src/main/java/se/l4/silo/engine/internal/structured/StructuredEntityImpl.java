package se.l4.silo.engine.internal.structured;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import se.l4.commons.io.Bytes;
import se.l4.commons.serialization.SerializerCollection;
import se.l4.commons.serialization.format.BinaryOutput;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.commons.serialization.format.Token;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.Storage;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;
import se.l4.silo.structured.ObjectEntity;
import se.l4.silo.structured.StructuredEntity;

public class StructuredEntityImpl
	implements StructuredEntity
{
	private final SerializerCollection serializers;
	private final String name;
	private final Storage entity;

	public StructuredEntityImpl(SerializerCollection serializers, String name, Storage entity)
	{
		this.serializers = serializers;
		this.name = name;
		this.entity = entity;
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public StoreResult store(Object id, StreamingInput out)
	{
		try
		{
			return entity.store(id, toBytes(out));
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to store; " + e.getMessage(), e);
		}
	}
	
	@Override
	public DeleteResult delete(Object id)
	{
		return entity.delete(id);
	}
	
	@Override
	public FetchResult<StreamingInput> get(Object id)
	{
		Bytes bytes = entity.get(id);
		if(bytes == null)
		{
			return FetchResult.empty();
		}
		
		
		return FetchResult.single(bytes).transform(BytesToStreamingInputFunction.INSTANCE);
	}
	
	@Override
	public <RT, Q extends Query<?>> Q query(String engine, QueryType<StreamingInput, RT, Q> type)
	{
		return type.create((data, translator) -> {
			return entity.query(engine, data, BytesToStreamingInputFunction.INSTANCE)
				.transform(translator);
		});
	}
	
	@Override
	public FetchResult<StreamingInput> stream()
	{
		return entity.stream().transform(BytesToStreamingInputFunction.INSTANCE);
	}
	
	@Override
	public <T> ObjectEntity<T> asObject(Class<T> type)
	{
		return asObject(serializers.find(type));
	}
	
	private Bytes toBytes(StreamingInput in)
		throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Tag with a version
		baos.write(0);
		
		BinaryOutput out = new BinaryOutput(baos);
		switch(in.next())
		{
			case OBJECT_START:
				copyObject(in, out, "");
				break;
			case LIST_START:
				copyList(in, out, "");
				break;
			case OBJECT_END:
			case LIST_END:
			case KEY:
				throw new IOException("The given input was invalid, structured data can not start with OBJECT_END, LIST_END or KEY");
			case NULL:
				out.writeNull("");
				break;
			case VALUE:
				copyValue(in, out, "");
				break;
		}
		
		out.flush();
		
		return Bytes.create(baos.toByteArray());
	}

	private void copyObject(StreamingInput in, BinaryOutput out, String key)
		throws IOException
	{
		out.writeObjectStart(key);
		while(in.peek() != Token.OBJECT_END)
		{
			in.next(Token.KEY);
			String subKey = in.getString();
			switch(in.next())
			{
				case OBJECT_START:
					copyObject(in, out, subKey);
					break;
				case LIST_START:
					copyList(in, out, subKey);
					break;
				case VALUE:
					copyValue(in, out, subKey);
					break;
				case NULL:
					out.writeNull(subKey);
					break;
				default:
					throw new IOException("Can not copy, unexpected token: " + in.current());
			}
		}
		in.next(Token.OBJECT_END);
		out.writeObjectEnd(key);
	}
	
	private void copyList(StreamingInput in, BinaryOutput out, String key)
		throws IOException
	{
		out.writeListStart(key);
		while(in.peek() != Token.LIST_END)
		{
			switch(in.next())
			{
				case OBJECT_START:
					copyObject(in, out, "entry");
					break;
				case LIST_START:
					copyList(in, out, "entry");
					break;
				case VALUE:
					copyValue(in, out, "entry");
					break;
				case NULL:
					out.writeNull("entry");
					break;
				default:
					throw new IOException("Can not copy, unexpected token: " + in.current());
			}
		}
		in.next(Token.LIST_END);
		out.writeListEnd(key);
	}
	
	private void copyValue(StreamingInput in, BinaryOutput out, String key)
		throws IOException
	{
		Object value = in.getValue();
		if(value instanceof String)
		{
			out.write(key, (String) value);
		}
		else if(value instanceof Long)
		{
			out.write(key, (Long) value);
		}
		else if(value instanceof Integer)
		{
			out.write(key, (Integer) value);
		}
		else if(value instanceof Double)
		{
			out.write(key, (Double) value);
		}
		else if(value instanceof Float)
		{
			out.write(key, (Float) value);
		}
		else if(value instanceof Boolean)
		{
			out.write(key, (Boolean) value);
		}
		else if(value instanceof byte[])
		{
			out.write(key, (byte[]) value);
		}
		else
		{
			throw new IOException("Unsupported value: " + value);
		}
	}

}
