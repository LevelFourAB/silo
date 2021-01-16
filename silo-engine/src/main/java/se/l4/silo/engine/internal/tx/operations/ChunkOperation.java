package se.l4.silo.engine.internal.tx.operations;

/**
 * Operation that includes chunked data.
 */
public interface ChunkOperation
	extends TransactionOperation
{
	/**
	 * Get the data of this chunk.
	 *
	 * @return
	 */
	byte[] getData();
}
