package se.l4.silo.engine.internal.structured;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.google.common.collect.Iterators;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.serialization.format.BinaryInput;
import se.l4.aurochs.serialization.format.StreamingInput;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;

public class StreamingInputFetchResult
	implements FetchResult<StreamingInput>
{
	private final Iterator<Bytes> it;
	private InputStream current;
	
	private final int size;
	private final int offset;
	private final int limit;

	public StreamingInputFetchResult(Iterator<Bytes> it, int size, int offset, int limit)
	{
		this.it = it;
		this.size = size;
		this.offset = offset;
		this.limit = limit;
	}
	
	public static FetchResult<StreamingInput> single(Bytes bytes)
	{
		return new StreamingInputFetchResult(Iterators.singletonIterator(bytes), 1, 0, 0);
	}

	@Override
	public Iterator<StreamingInput> iterator()
	{
		return new Iterator<StreamingInput>()
		{

			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}
			
			@Override
			public StreamingInput next()
			{
				try
				{
					if(current != null)
					{
						current.close();
					}
					
					Bytes bytes = it.next();
					current = bytes.asInputStream();
					int version = current.read();
					if(version != 0)
					{
						throw new StorageException("Data has an unknown version: " + version + ". Data is corrupt.");
					}
					return new BinaryInput(current);
				}
				catch(IOException e)
				{
					throw new StorageException("Unable to get the next result; " + e.getMessage(), e);
				}
			}
		};
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public int getOffset()
	{
		return offset;
	}

	@Override
	public int getLimit()
	{
		return limit;
	}

	@Override
	public boolean isEmpty()
	{
		return size == 0;
	}

	@Override
	public void close()
	{
		if(current != null)
		{
			try
			{
				current.close();
			}
			catch(IOException e)
			{
				throw new StorageException("Unable to close; " + e.getMessage(), e);
			}
		}
	}

}
