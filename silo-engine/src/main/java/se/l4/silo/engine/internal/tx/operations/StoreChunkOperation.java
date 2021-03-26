package se.l4.silo.engine.internal.tx.operations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

/**
 * Operation indicating something is being stored in a collection.
 */
public class StoreChunkOperation
	implements ChunkOperation
{
	private final String collection;
	private final Object id;
	private final byte[] chunk;

	public StoreChunkOperation(
		String collection,
		Object id,
		byte[] chunk
	)
	{
		this.collection = collection;
		this.id = id;
		this.chunk = chunk;
	}

	public String getCollection()
	{
		return collection;
	}

	public Object getId()
	{
		return id;
	}

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

		// Id
		result += TransactionOperation.estimateIdMemory(id);

		// Data
		result += 64 + chunk.length * 4;

		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(chunk);
		result = prime * result + Objects.hash(collection, id);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		StoreChunkOperation other = (StoreChunkOperation) obj;
		return Arrays.equals(chunk, other.chunk)
			&& Objects.equals(collection, other.collection)
			&& Objects.equals(id, other.id);
	}

	@Override
	public String toString()
	{
		return "StoreOperation{collection=" + collection + ", id=" + id + ", chunkSize=" + chunk.length + "}";
	}

	/**
	 * Create an instance of this operation.
	 *
	 * @param collection
	 * @param id
	 * @param chunk
	 * @return
	 */
	public static StoreChunkOperation create(
		String collection,
		Object id,
		byte[] chunk
	)
	{
		return new StoreChunkOperation(collection, id, chunk);
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
	public static StoreChunkOperation read(
		BinaryDataInput in
	)
		throws IOException
	{
		String collection = in.readString();
		Object id = in.readId();
		byte[] data = in.readByteArray();

		return new StoreChunkOperation(collection, id, data);
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
		StoreChunkOperation op
	)
		throws IOException
	{
		write(
			out,
			op.getCollection(),
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
	 * @param id
	 * @param chunk
	 * @param chunkOffset
	 * @param chunkLength
	 * @throws IOException
	 */
	public static void write(
		BinaryDataOutput out,
		String collection,
		Object id,
		byte[] chunk,
		int chunkOffset,
		int chunkLength
	)
		throws IOException
	{
		out.writeString(collection);
		out.writeId(id);
		out.writeByteArray(chunk, chunkOffset, chunkLength);
	}

	/**
	 * Write an operation to the given output.
	 *
	 * @param out
	 * @param collection
	 * @param id
	 * @throws IOException
	 */
	public static void writeEnd(
		BinaryDataOutput out,
		String collection,
		Object id
	)
		throws IOException
	{
		out.writeString(collection);
		out.writeId(id);
		out.writeVInt(0);
	}
}
