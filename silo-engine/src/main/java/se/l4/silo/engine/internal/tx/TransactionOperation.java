package se.l4.silo.engine.internal.tx;

import se.l4.silo.engine.internal.TransactionAdapter;

/**
 * Operation in a transaction. Used in {@link TransactionAdapter} to
 * store information about the operations a transaction wants to perform.
 *
 * @author Andreas Holstenson
 *
 */
public class TransactionOperation
{
	public static enum Type
	{
		COMMIT,
		ROLLBACK,
		STORE_CHUNK,
		DELETE,
		START
	}

	private final Type type;
	private final long timestamp;
	private final String entity;
	private final Object id;
	private final byte[] data;

	private TransactionOperation(Type type, long timestamp, String entity, Object id, byte[] data)
	{
		this.type = type;
		this.timestamp = timestamp;
		this.entity = entity;
		this.id = id;
		this.data = data;
	}

	public Type getType()
	{
		return type;
	}

	public String getEntity()
	{
		return entity;
	}

	public Object getId()
	{
		return id;
	}

	public byte[] getData()
	{
		return data;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public static TransactionOperation start(long timestamp)
	{
		return new TransactionOperation(Type.START, timestamp, null, null, null);
	}

	public static TransactionOperation commit()
	{
		return new TransactionOperation(Type.COMMIT, 0, null, null, null);
	}

	public static TransactionOperation rollback()
	{
		return new TransactionOperation(Type.ROLLBACK, 0, null, null, null);
	}

	public static TransactionOperation store(String entity, Object id, byte[] chunk)
	{
		return new TransactionOperation(Type.STORE_CHUNK, 0, entity, id, chunk);
	}

	public static TransactionOperation delete(String entity, Object id)
	{
		return new TransactionOperation(Type.DELETE, 0, entity, id, null);
	}
}
