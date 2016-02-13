package se.l4.silo.engine.log;

import se.l4.aurochs.core.io.Bytes;

/**
 * Implementation of {@link LogEntry}.
 * 
 * @author Andreas Holstenson
 *
 */
public class DefaultLogEntry
	implements LogEntry
{
	private final long timestamp;
	private final Bytes data;

	public DefaultLogEntry(long timestamp, Bytes data)
	{
		this.timestamp = timestamp;
		this.data = data;
	}

	@Override
	public long getTimestamp()
	{
		return timestamp;
	}

	@Override
	public Bytes getData()
	{
		return data;
	}

}
