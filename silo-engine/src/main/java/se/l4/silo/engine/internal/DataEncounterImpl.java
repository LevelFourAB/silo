package se.l4.silo.engine.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.serialization.format.BinaryInput;
import se.l4.aurochs.serialization.format.StreamingInput;
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
			return new BinaryInput(stream);
		}
		catch(IOException e)
		{
			throw new StorageException("Can not read data from storage during query engine update; " + e.getMessage(), e);
		}
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
}
