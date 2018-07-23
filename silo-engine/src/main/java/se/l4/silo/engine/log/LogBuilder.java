package se.l4.silo.engine.log;

import se.l4.commons.io.IoConsumer;

/**
 * Builder that can create an instance of a {@link Log}.
 *
 * @author Andreas Holstenson
 *
 */
public interface LogBuilder
{
	/**
	 * Create a new log with the given consumer of entries. The consumer will
	 * be called whenever some new log data is available.
	 *
	 * @param data
	 * @return
	 */
	Log build(IoConsumer<LogEntry> consumer);
}
