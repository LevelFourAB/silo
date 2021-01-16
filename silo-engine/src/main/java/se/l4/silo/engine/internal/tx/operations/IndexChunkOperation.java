package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation representing a chunk of data in an index.
 */
public class IndexChunkOperation
	implements ChunkOperation
{
	private final String entity;
	private final String index;
	private final Object id;
	private final byte[] chunk;

	private IndexChunkOperation(
		String entity,
		String index,
		Object id,
		byte[] chunk
	)
	{
		this.entity = entity;
		this.index = index;
		this.id = id;
		this.chunk = chunk;
	}

	/**
	 * Get entity this operation applies to.
	 *
	 * @return
	 */
	public String getEntity()
	{
		return entity;
	}

	/**
	 * Get the index this data is for.
	 *
	 * @return
	 */
	public String getIndex()
	{
		return index;
	}

	/**
	 * Get the identifier.
	 *
	 * @return
	 */
	public Object getId()
	{
		return id;
	}

	/**
	 * Get the binary data.
	 *
	 * @return
	 */
	public byte[] getData()
	{
		return chunk;
	}

	@Override
	public int estimateMemory()
	{
		int result = 8;

		// Entity
		result += 24 + 2 * entity.length();

		// Index
		result += 24 + 2 * index.length();

		// Id
		result += TransactionOperation.estimateIdMemory(id);

		// Data
		result += 64 + chunk.length * 4;

		return result;
	}

	/**
	 * Create an instance of this operation.
	 *
	 * @param entity
	 * @param index
	 * @param id
	 * @param chunk
	 * @return
	 */
	public static IndexChunkOperation create(
		String entity,
		String index,
		Object id,
		byte[] chunk
	)
	{
		return new IndexChunkOperation(entity, index, id, chunk);
	}

	/**
	 * Read this operation from the given input.
	 *
	 * @param in
	 *   input to read from
	 * @return
	 *   read operation
	 * @throws IOException
	 */
	public static IndexChunkOperation read(
		BinaryDataInput in
	)
		throws IOException
	{
		String entity = in.readString();
		String index = in.readString();
		Object id = in.readId();
		byte[] data = in.readByteArray();

		return new IndexChunkOperation(entity, index, id, data);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 *   output to write to
	 * @param op
	 *   the operation to write
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		IndexChunkOperation op
	)
		throws IOException
	{
		write(
			out,
			op.getEntity(),
			op.getIndex(),
			op.getId(),
			op.getData(),
			0,
			op.getData().length
		);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param entity
	 * @param index
	 * @param id
	 * @param chunk
	 * @param chunkOffset
	 * @param chunkLength
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		String entity,
		String index,
		Object id,
		byte[] chunk,
		int chunkOffset,
		int chunkLength
	)
		throws IOException
	{
		out.writeString(entity);
		out.writeString(index);
		out.writeId(id);
		out.writeByteArray(chunk, chunkOffset, chunkLength);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param entity
	 * @param index
	 * @param id
	 * @throws IOException
	 */
	public static void writeEnd(
		BinaryDataOutput out,
		String entity,
		String index,
		Object id
	)
		throws IOException
	{
		out.writeString(entity);
		out.writeString(index);
		out.writeId(id);
		out.writeVInt(0);
	}
}
