package se.l4.silo.engine.log;

import java.io.IOException;

import se.l4.ylem.io.Bytes;
import se.l4.ylem.io.IOConsumer;

/**
 * Log that simply forwards whatever is appended to the consumer of entries.
 * This log provides no data consistency in the form of storage to disk.
 *
 * @author Andreas Holstenson
 *
 */
public class DirectApplyLog
	implements Log
{
	private final IOConsumer<LogEntry> consumer;
	private transient boolean closed;

	private DirectApplyLog(IOConsumer<LogEntry> consumer)
	{
		this.consumer = consumer;
	}

	@Override
	public void append(Bytes bytes)
		throws IOException
	{
		if(closed)
		{
			throw new IOException("Log has been closed");
		}

		consumer.accept(new DefaultLogEntry(System.currentTimeMillis(), bytes));
	}

	@Override
	public void close()
		throws IOException
	{
		closed = true;
	}

	/**
	 * Start building a new direct apply log.
	 *
	 * @return
	 */
	public static LogBuilder builder()
	{
		return new LogBuilder()
		{
			@Override
			public Log build(IOConsumer<LogEntry> consumer)
			{
				return new DirectApplyLog(consumer);
			}
		};
	}
}
