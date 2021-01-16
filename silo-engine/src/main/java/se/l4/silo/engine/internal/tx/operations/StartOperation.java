package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;
import java.util.Objects;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation that starts a transaction.
 */
public class StartOperation
	implements TransactionOperation
{
	private final long timestamp;

	public StartOperation(
		long timestamp
	)
	{
		this.timestamp = timestamp;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	@Override
	public int estimateMemory()
	{
		return 8 + 8;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(timestamp);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		StartOperation other = (StartOperation) obj;
		return timestamp == other.timestamp;
	}

	@Override
	public String toString()
	{
		return "StartOperation{timestamp=" + timestamp + "}";
	}

	/**
	 * Create a new operation.
	 *
	 * @param timestamp
	 * @return
	 */
	public static StartOperation create(
		long timestamp
	)
	{
		return new StartOperation(timestamp);
	}

	/**
	 * Read an operation from the given input.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static StartOperation read(
		BinaryDataInput in
	)
		throws IOException
	{
		long timestamp = in.readVLong();

		return new StartOperation(timestamp);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param op
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		StartOperation op
	)
		throws IOException
	{
		write(out, op.getTimestamp());
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param timestamp
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		long timestamp
	)
		throws IOException
	{
		out.writeVLong(timestamp);
	}
}
