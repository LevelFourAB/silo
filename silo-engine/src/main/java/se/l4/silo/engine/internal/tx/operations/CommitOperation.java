package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation representing a commit.
 */
public class CommitOperation
	implements TransactionOperation
{
	private static final CommitOperation INSTANCE = new CommitOperation();

	@Override
	public int estimateMemory()
	{
		return 8;
	}

	/**
	 * Create an instance of this operation.
	 *
	 * @return
	 */
	public static CommitOperation create()
	{
		return INSTANCE;
	}

	/**
	 * Read this operation from the given input.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static CommitOperation read(
		BinaryDataInput in
	)
		throws IOException
	{
		return INSTANCE;
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
		CommitOperation op
	)
		throws IOException
	{
		write(out);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out
	)
		throws IOException
	{
	}
}
