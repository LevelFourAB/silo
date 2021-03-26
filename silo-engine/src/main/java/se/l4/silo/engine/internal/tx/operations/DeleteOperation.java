package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;
import java.util.Objects;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation representing the deletion of a certain id in a collection.
 */
public class DeleteOperation
	implements TransactionOperation
{
	private final String collection;
	private final Object id;

	public DeleteOperation(
		String collection,
		Object id
	)
	{
		this.collection = collection;
		this.id = id;
	}

	public String getCollection()
	{
		return collection;
	}

	public Object getId()
	{
		return id;
	}

	@Override
	public int estimateMemory()
	{
		int result = 8;

		// collection
		result += 24 + 2 * collection.length();

		// Id
		result += TransactionOperation.estimateIdMemory(id);

		return result;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(collection, id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		DeleteOperation other = (DeleteOperation) obj;
		return Objects.equals(collection, other.collection)
			&& Objects.equals(id, other.id);
	}

	@Override
	public String toString()
	{
		return "DeleteOperation{collection=" + collection + ", id=" + id + "}";
	}

	/**
	 * Create an instance of this operation.
	 *
	 * @return
	 */
	public static DeleteOperation create(
		String collection,
		Object id
	)
	{
		return new DeleteOperation(collection, id);
	}

	/**
	 * Read this operation from the given input.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static DeleteOperation read(
		BinaryDataInput in
	)
		throws IOException
	{
		String collection = in.readString();
		Object id = in.readId();

		return new DeleteOperation(collection, id);
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
		DeleteOperation op
	)
		throws IOException
	{
		write(out, op.getCollection(), op.getId());
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param collection
	 * @param id
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		String collection,
		Object id
	)
		throws IOException
	{
		out.writeString(collection);
		out.writeId(id);
	}
}
