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
	private final String collection;
	private final String index;
	private final Object id;
	private final byte[] chunk;

	private IndexChunkOperation(
		String collection,
		String index,
		Object id,
		byte[] chunk
	)
	{
		this.collection = collection;
		this.index = index;
		this.id = id;
		this.chunk = chunk;
	}

	/**
	 * Get collection this operation applies to.
	 *
	 * @return
	 */
	public String getCollection()
	{
		return collection;
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

		// collection
		result += 24 + 2 * collection.length();

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
	 * @param collection
	 * @param index
	 * @param id
	 * @param chunk
	 * @return
	 */
	public static IndexChunkOperation create(
		String collection,
		String index,
		Object id,
		byte[] chunk
	)
	{
		return new IndexChunkOperation(collection, index, id, chunk);
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
		String collection = in.readString();
		String index = in.readString();
		Object id = in.readId();
		byte[] data = in.readByteArray();

		return new IndexChunkOperation(collection, index, id, data);
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
			op.getCollection(),
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
	 * @param collection
	 * @param index
	 * @param id
	 * @param chunk
	 * @param chunkOffset
	 * @param chunkLength
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		String collection,
		String index,
		Object id,
		byte[] chunk,
		int chunkOffset,
		int chunkLength
	)
		throws IOException
	{
		out.writeString(collection);
		out.writeString(index);
		out.writeId(id);
		out.writeByteArray(chunk, chunkOffset, chunkLength);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param collection
	 * @param index
	 * @param id
	 * @throws IOException
	 */
	public static void writeEnd(
		BinaryDataOutput out,
		String collection,
		String index,
		Object id
	)
		throws IOException
	{
		out.writeString(collection);
		out.writeString(index);
		out.writeId(id);
		out.writeVInt(0);
	}
}
