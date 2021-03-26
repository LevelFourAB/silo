package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.l4.exobytes.Serializer;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.ObjectCodec;

/**
 * {@link ObjectCodec} that uses a {@link Serializer} to encode and decode
 * data. Supports backwards compatibility with the now removed
 * {@code StructuredEntity}.
 */
public class SerializingCodec<T>
	implements ObjectCodec<T>
{
	private final Serializer<T> serializer;

	public SerializingCodec(
		Serializer<T> serializer
	)
	{
		this.serializer = serializer;
	}

	@Override
	public T decode(InputStream in)
		throws IOException
	{
		int version = in.read();

		StreamingFormat format;
		switch(version)
		{
			case 0:
				// Legacy format, as used in earlier versions of Silo
				format = StreamingFormat.LEGACY_BINARY;
				break;
			case 1:
				// CBOR
				format = StreamingFormat.CBOR;
				break;
			default:
				throw new IOException("Unsupported data, unknown version received. Data indicates version " + version + ", but only version 0 or 1 is supported");
		}

		try(StreamingInput streaming = format.createInput(in))
		{
			return streaming.readObject(serializer);
		}
	}

	@Override
	public void encode(T instance, OutputStream out)
		throws IOException
	{
		// Indicate CBOR format
		out.write(1);

		try(StreamingOutput streaming = StreamingFormat.CBOR.createOutput(out))
		{
			streaming.writeObject(serializer, instance);
		}
	}
}
