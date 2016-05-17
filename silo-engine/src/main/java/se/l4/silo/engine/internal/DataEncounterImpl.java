package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableSet;

import se.l4.commons.io.Bytes;
import se.l4.commons.io.IoConsumer;
import se.l4.commons.io.IoFunction;
import se.l4.commons.serialization.format.BinaryInput;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.commons.serialization.format.Token;
import se.l4.silo.StorageException;
import se.l4.silo.engine.DataEncounter;
import se.l4.silo.engine.Storage;

public class DataEncounterImpl
	implements DataEncounter, AutoCloseable
{
	private final StorageEngine engine;
	
	private final Bytes data;
	private final List<Closeable> opened;

	public DataEncounterImpl(StorageEngine engine, Bytes data)
	{
		this.engine = engine;
		this.data = data;
		opened = new ArrayList<>();
	}

	@Override
	public Bytes getData()
	{
		return data;
	}

	@Override
	public StreamingInput asStructured()
	{
		try
		{
			InputStream stream = data.asInputStream();
			opened.add(stream);
			
			int tag = stream.read();
			if(tag != 0)
			{
				throw new StorageException("Unknown storage version: " + tag + ", this version of Silo is either old or the data is corrupt");
			}
			
			return new BinaryInput(stream);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not read data from storage during query engine update; " + e.getMessage(), e);
		}
	}
	
	public <T> T withStreamingInput(IoFunction<StreamingInput, T> func)
	{
		try(InputStream stream = data.asInputStream())
		{
			int tag = stream.read();
			if(tag != 0)
			{
				throw new StorageException("Unknown storage version: " + tag + ", this version of Silo is either old or the data is corrupt");
			}
			
			BinaryInput in = new BinaryInput(stream);
			return func.apply(in);
		}
		catch(IOException e)
		{
			throw new StorageException("Could not read data from storage during query engine update; " + e.getMessage(), e);
		}
	}
	
	@Override
	public Map<String, Object> findStructuredKeys(Collection<String> keys)
	{
		return withStreamingInput(in -> {
			Map<String, Object> result = new HashMap<>();
			
			new StructuredKeyFetcher(ImmutableSet.copyOf(keys), result::put).accept(in);
			
			return result;
		});
	}
	
	@Override
	public void findStructuredKeys(Collection<String> keys, BiConsumer<String, Object> receiver)
	{
		withStreamingInput(in -> {
			new StructuredKeyFetcher(ImmutableSet.copyOf(keys), receiver).accept(in);
			
			return null;
		});
	}
	
	@Override
	public Object[] getStructuredArray(String[] keys)
	{
		return getStructuredArray(keys, 0);
	}
	
	@Override
	public Object[] getStructuredArray(String[] keys, int appendCount)
	{
		return withStreamingInput(in -> {
			Object[] result = new Object[keys.length + appendCount];
			
			new StructuredKeyFetcher(ImmutableSet.copyOf(keys), (k, v) -> {
				for(int i=0, n=keys.length; i<n; i++)
				{
					if(keys[i].equals(k))
					{
						result[i] = v;
						break;
					}
				}
			}).accept(in);
			
			return result;
		});
	}
	
	@Override
	public Storage getStorage(String entity)
	{
		return engine.getStorage(entity);
	}
	
	@Override
	public Storage getStorage(String entity, String name)
	{
		return engine.getStorage(entity, name);
	}

	@Override
	public void close()
	{
		for(Closeable c : opened)
		{
			try
			{
				c.close();
			}
			catch(IOException e)
			{
			}
		}
	}
	
	private static class StructuredKeyFetcher
		implements IoConsumer<StreamingInput>
	{
		private Set<String> keys;
		private BiConsumer<String, Object> receiver;
		
		public StructuredKeyFetcher(Set<String> keys, BiConsumer<String, Object> receiver)
		{
			this.keys = keys;
			this.receiver = receiver;
		}

		@Override
		public void accept(StreamingInput in)
			throws IOException
		{
			handle(in, null);
		}
		
		private void handle(StreamingInput in, String key)
			throws IOException
		{
			Token next = in.peek();
			switch(next)
			{
				case OBJECT_START:
					handleObject(in, key);
					break;
				case LIST_START:
					handleList(in, key);
					break;
				case VALUE:
					handleValue(in, key);
					break;
				case NULL:
					handleNull(in, key);
					break;
				default:
					throw new IOException("Structured data invalid, got a token out of order: " + next);
			}
		}
		
		private void handleObject(StreamingInput in, String key)
			throws IOException
		{
			in.next(Token.OBJECT_START);
			
			while(in.peek() != Token.OBJECT_END)
			{
				in.next(Token.KEY);
				String localKey = in.getString();
				
				String subKey = key(key, localKey);
				handle(in, subKey);
			}
			
			in.next(Token.OBJECT_END);
		}
		
		private void handleList(StreamingInput in, String key)
			throws IOException
		{
			in.next(Token.LIST_START);
			
			while(in.peek() != Token.LIST_END)
			{
				handle(in, key);
			}
			
			in.next(Token.LIST_END);
		}
		
		private void handleValue(StreamingInput in, String key)
			throws IOException
		{
			in.next(Token.VALUE);
			
			if(keys.contains(key))
			{
				receiver.accept(key, in.getValue());
			}
		}
		
		private void handleNull(StreamingInput in, String key)
			throws IOException
		{
			in.next(Token.NULL);
			
			if(keys.contains(key))
			{
				receiver.accept(key, null);
			}
		}
			
		private String key(String current, String sub)
		{
			return current == null ? sub : current + '.' + sub;
		}
	}
}
