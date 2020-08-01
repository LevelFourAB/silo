package se.l4.silo.engine.internal.structured;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.silo.StorageException;
import se.l4.ylem.io.Bytes;

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

			return StreamingFormat.LEGACY_BINARY.createInput(current);
		}
		catch(IOException e)
		{
			throw new StorageException("I/O error during transformation from Bytes to StreamingInput");
		}
	}

}
