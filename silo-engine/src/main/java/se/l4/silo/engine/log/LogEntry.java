package se.l4.silo.engine.log;

import se.l4.aurochs.core.io.Bytes;

/**
 * Entry in a {@link Log log}, containing the actual logged data together
 * with metadata.
 * 
 * @author Andreas Holstenson
 *
 */
public interface LogEntry
{
	/**
	 * Get the timestamp when the log entry was received.
	 * 
	 * @return
	 */
	long getTimestamp();
	
	/**
	 * Get the data of this entry.
	 * 
	 * @return
	 */
	Bytes getData();
}
