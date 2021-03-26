package se.l4.silo.engine.internal.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.StorageException;
import se.l4.silo.engine.internal.MessageConstants;
import se.l4.silo.engine.internal.tx.operations.DeleteOperation;
import se.l4.silo.engine.internal.tx.operations.IndexChunkOperation;
import se.l4.silo.engine.internal.tx.operations.StartOperation;
import se.l4.silo.engine.internal.tx.operations.StoreChunkOperation;
import se.l4.silo.engine.io.BinaryDataOutput;
import se.l4.silo.engine.log.Log;
import se.l4.ylem.ids.LongIdGenerator;
import se.l4.ylem.io.Bytes;
import se.l4.ylem.io.IOConsumer;

/**
 * Implementation of {@link TransactionLog} that translates our transaction
 * semantics into messages written to a {@link Log log}.
 *
 * @author Andreas Holstenson
 *
 */
public class TransactionLogImpl
	implements TransactionLog
{
	/**
	 * The size used for chunks in the log.
	 */
	private static final int CHUNK_SIZE = 8192;

	private static final Logger logger = LoggerFactory.getLogger(TransactionLogImpl.class);

	private final Log log;
	private final LongIdGenerator ids;

	public TransactionLogImpl(Log log, LongIdGenerator ids)
	{
		this.log = log;
		this.ids = ids;
	}

	@Override
	public long startTransaction()
	{
		long tx = ids.next();
		try
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Transaction started");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);

				out.write(MessageConstants.START_TRANSACTION);
				out.writeVLong(tx);

				StartOperation.write(out, System.currentTimeMillis());
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not start transaction, log said: " + e.getMessage(), e);
		}

		return tx;
	}

	@Override
	public void store(
		long tx,
		String collection,
		Object id,
		IOConsumer<OutputStream> generator
	)
	{
		try
		{
			ChunkOutputStream.Control control = (data, offset, length) -> {
				if(logger.isTraceEnabled())
				{
					logger.trace("[" + tx + "] Wrote chunk for " + collection + "[" + id + "]: " + Base64.getEncoder().encodeToString(data));
				}

				log.append(Bytes.capture(stream -> {
					BinaryDataOutput out = BinaryDataOutput.forStream(stream);

					out.write(MessageConstants.STORE_CHUNK);
					out.writeVLong(tx);

					StoreChunkOperation.write(out, collection, id, data, offset, length);
				}));
			};

			OutputStream chunkOutput = new ChunkOutputStream(new byte[CHUNK_SIZE], control);

			// Ask the generator to write data
			generator.accept(chunkOutput);

			// Close and flush the output
			chunkOutput.close();

			// Write a zero length chunk to indicate end of object
			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Wrote end of data for " + collection + "[" + id + "]");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);
				out.write(MessageConstants.STORE_CHUNK);
				out.writeVLong(tx);

				StoreChunkOperation.writeEnd(out, collection, id);
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not store " + collection + " with id " + id + " in transaction" + tx + "; " + e.getMessage(), e);
		}
	}

	@Override
	public void delete(long tx, String collection, Object id)
	{
		try
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Wrote delete for " + collection + "[" + id + "]");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);

				out.write(MessageConstants.DELETE);
				out.writeVLong(tx);

				DeleteOperation.write(out, collection, id);
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not delete " + collection + " with id " + id + " in transaction" + tx + ", log said: " + e.getMessage(), e);
		}
	}

	@Override
	public void storeIndex(
		long tx,
		String collection,
		String index,
		Object id,
		IOConsumer<OutputStream> generator
	)
	{
		try
		{
			ChunkOutputStream.Control control = (data, offset, length) -> {
				if(logger.isTraceEnabled())
				{
					logger.trace("[" + tx + "] Wrote index chunk for " + collection + "[" + id + "]: " + Base64.getEncoder().encodeToString(data));
				}

				log.append(Bytes.capture(stream -> {
					BinaryDataOutput out = BinaryDataOutput.forStream(stream);

					out.write(MessageConstants.INDEX_CHUNK);
					out.writeVLong(tx);

					IndexChunkOperation.write(
						out,
						collection,
						index,
						id,
						data,
						offset,
						length
					);
				}));
			};

			OutputStream chunkOutput = new ChunkOutputStream(new byte[CHUNK_SIZE], control);

			// Ask the generator to write output
			generator.accept(chunkOutput);

			// Close and flush the output
			chunkOutput.close();

			// Write a zero length chunk to indicate end of index

			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Wrote end of data for " + collection + "[" + id + "]");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);
				out.write(MessageConstants.INDEX_CHUNK);
				out.writeVLong(tx);

				IndexChunkOperation.writeEnd(
					out,
					collection,
					index,
					id
				);
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not store index " + index + " in " + collection + " with id " + id + " in transaction" + tx + "; " + e.getMessage(), e);
		}
	}


	@Override
	public void commitTransaction(long tx)
	{
		try
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Transaction committed");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);
				out.write(MessageConstants.COMMIT_TRANSACTION);
				out.writeVLong(tx);
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not commit transaction " +  tx + ", log said: " + e.getMessage(), e);
		}
	}

	@Override
	public void rollbackTransaction(long tx)
	{
		try
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("[" + tx + "] Transaction rolled back");
			}

			log.append(Bytes.capture(stream -> {
				BinaryDataOutput out = BinaryDataOutput.forStream(stream);
				out.write(MessageConstants.ROLLBACK_TRANSACTION);
				out.writeVLong(tx);
			}));
		}
		catch(IOException e)
		{
			throw new StorageException("Could not rollback transaction " +  tx + ", log said: " + e.getMessage(), e);
		}
	}
}
