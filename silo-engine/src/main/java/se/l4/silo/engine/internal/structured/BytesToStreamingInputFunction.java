package se.l4.silo.engine.internal.structured;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import se.l4.commons.io.Bytes;
import se.l4.commons.serialization.format.BinaryInput;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.silo.StorageException;

public class BytesToStreamingInputFunction
	implements Function<Bytes, StreamingInput>
{
	public static final BytesToStreamingInputFunction INSTANCE = new BytesToStreamingInputFunction();

	public BytesToStreamingInputFunction()
	{
	}

	@Override
	public StreamingInput apply(Bytes bytes)
	{
		try
		{
			InputStream current = bytes.asInputStream();
			int version = current.read();
			if(version != 0)
			{
				current.close();
				throw new StorageException("Data has an unknown version: " + version + ". Data is corrupt.");
			}

			return new BinaryInput(current);
		}
		catch(IOException e)
		{
			throw new StorageException("I/O error during transformation from Bytes to StreamingInput");
		}
	}

}
