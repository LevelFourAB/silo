package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.core.io.IoConsumer;
import se.l4.aurochs.core.io.IoFunction;
import se.l4.aurochs.serialization.format.BinaryInput;
import se.l4.aurochs.serialization.format.StreamingInput;
import se.l4.aurochs.serialization.format.StreamingInput.Token;
import se.l4.silo.StorageException;
import se.l4.silo.engine.DataEncounter;

public class DataEncounterImpl
	implements DataEncounter, AutoCloseable
{
	private final Bytes data;
	private final List<Closeable> opened;

	public DataEncounterImpl(Bytes data)
	{
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
		// TODO Auto-generated method stub
		return null;
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
	
	private interface KeyValueReceiver
	{
		void receive(String key, Object value);
	}
	
	private static class StructuredKeyFetcher
		implements IoConsumer<StreamingInput>
	{
		private Set<String> keys;
		private KeyValueReceiver receiver;
		
		private String key;
		private int listDepth;
		
		public StructuredKeyFetcher(Set<String> keys, KeyValueReceiver receiver)
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
				receiver.receive(key, in.getValue());
			}
		}
		
		private void handleNull(StreamingInput in, String key)
			throws IOException
		{
			in.next(Token.NULL);
			
			if(keys.contains(key))
			{
				receiver.receive(key, null);
			}
		}
			
		private String key(String current, String sub)
		{
			return current == null ? sub : current + '.' + sub;
		}
	}
}
