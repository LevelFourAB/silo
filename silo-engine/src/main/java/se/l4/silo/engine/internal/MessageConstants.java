package se.l4.silo.engine.internal;

import se.l4.silo.engine.log.LogEntry;

/**
 * Constants used for {@link LogEntry log entries}.
 *
 * @author Andreas Holstenson
 *
 */
public class MessageConstants
{
	public static final int START_TRANSACTION = 1;
	public static final int COMMIT_TRANSACTION = 2;
	public static final int ROLLBACK_TRANSACTION = 3;

	public static final int STORE_CHUNK = 10;
	public static final int DELETE = 11;

	private MessageConstants()
	{
	}
}
