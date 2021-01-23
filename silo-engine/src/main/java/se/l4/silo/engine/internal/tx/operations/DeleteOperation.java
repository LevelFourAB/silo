package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;
import java.util.Objects;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation representing the deletion of a certain id in an entity.
 */
public class DeleteOperation
	implements TransactionOperation
{
	private final String entity;
	private final Object id;

	public DeleteOperation(
		String entity,
		Object id
	)
	{
		this.entity = entity;
		this.id = id;
	}

	public String getEntity()
	{
		return entity;
	}

	public Object getId()
	{
		return id;
	}

	@Override
	public int estimateMemory()
	{
		int result = 8;

		// Entity
		result += 24 + 2 * entity.length();

		// Id
		result += TransactionOperation.estimateIdMemory(id);

		return result;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entity, id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		DeleteOperation other = (DeleteOperation) obj;
		return Objects.equals(entity, other.entity)
			&& Objects.equals(id, other.id);
	}

	@Override
	public String toString()
	{
		return "DeleteOperation{entity=" + entity + ", id=" + id + "}";
	}

	/**
	 * Create an instance of this operation.
	 *
	 * @return
	 */
	public static DeleteOperation create(
		String entity,
		Object id
	)
	{
		return new DeleteOperation(entity, id);
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
		String entity = in.readString();
		Object id = in.readId();

		return new DeleteOperation(entity, id);
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
		write(out, op.getEntity(), op.getId());
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param entity
	 * @param id
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		String entity,
		Object id
	)
		throws IOException
	{
		out.writeString(entity);
		out.writeId(id);
	}
}
