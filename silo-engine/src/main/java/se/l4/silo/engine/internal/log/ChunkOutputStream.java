package se.l4.silo.engine.internal.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class ChunkOutputStream
	extends OutputStream
{
	private final Control consumer;

	private final byte[] buffer;
	private int bufferIndex;

	public ChunkOutputStream(
		int bufferSize,
		Control out
	)
	{
		this.consumer = Objects.requireNonNull(out);
		buffer = new byte[bufferSize];
	}

	@Override
	public void write(int b)
		throws IOException
	{
		buffer[bufferIndex++] = (byte) b;
		if(bufferIndex == buffer.length)
		{
			consumer.consume(buffer, 0, bufferIndex);
			bufferIndex = 0;
		}
	}

	@Override
	public void write(byte[] b, int localOffset, int localLength)
		throws IOException
	{
		int localIndex = 0;
		while(localIndex < localLength)
		{
			int available = Math.min(localLength - localIndex, buffer.length - this.bufferIndex);
			System.arraycopy(b, localIndex, buffer, bufferIndex, available);

			this.bufferIndex += available;
			localIndex += available;

			if(bufferIndex == buffer.length)
			{
				consumer.consume(buffer, 0, bufferIndex);
				bufferIndex = 0;
			}
		}
	}

	@Override
	public void flush()
		throws IOException
	{
		if(bufferIndex != 0)
		{
			consumer.consume(buffer, 0, bufferIndex);
			bufferIndex = 0;
		}
	}

	@Override
	public void close()
		throws IOException
	{
		if(bufferIndex != 0)
		{
			consumer.consume(buffer, 0, bufferIndex);
			bufferIndex = 0;
		}
	}

	public interface Control
	{
		void consume(byte[] data, int offset, int length)
			throws IOException;
	}
}
