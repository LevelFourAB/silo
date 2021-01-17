package se.l4.silo.engine.internal.tx;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import se.l4.silo.StorageException;
import se.l4.silo.engine.internal.MessageConstants;
import se.l4.silo.engine.internal.tx.operations.CommitOperation;
import se.l4.silo.engine.internal.tx.operations.DeleteOperation;
import se.l4.silo.engine.internal.tx.operations.IndexChunkOperation;
import se.l4.silo.engine.internal.tx.operations.RollbackOperation;
import se.l4.silo.engine.internal.tx.operations.StartOperation;
import se.l4.silo.engine.internal.tx.operations.StoreChunkOperation;
import se.l4.silo.engine.internal.tx.operations.TransactionOperation;
import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;

public class TransactionOperationType
	implements DataType
{
	@Override
	public int compare(Object o1, Object o2)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMemory(Object instance)
	{
		return ((TransactionOperation) instance).estimateMemory();
	}

	@Override
	public void write(WriteBuffer buf, Object object)
	{
		try
		{
			BinaryDataOutput out = BinaryDataOutput.forBuffer(buf);

			if(object instanceof StartOperation)
			{
				out.write(MessageConstants.START_TRANSACTION);

				StartOperation.write(out, (StartOperation) object);
			}
			else if(object instanceof CommitOperation)
			{
				out.write(MessageConstants.COMMIT_TRANSACTION);
			}
			else if(object instanceof RollbackOperation)
			{
				out.write(MessageConstants.ROLLBACK_TRANSACTION);
			}
			else if(object instanceof DeleteOperation)
			{
				out.write(MessageConstants.DELETE);

				DeleteOperation.write(out, (DeleteOperation) object);
			}
			else if(object instanceof StoreChunkOperation)
			{
				out.write(MessageConstants.STORE_CHUNK);

				StoreChunkOperation.write(out, (StoreChunkOperation) object);
			}
			else if(object instanceof IndexChunkOperation)
			{
				out.write(MessageConstants.INDEX_CHUNK);

				IndexChunkOperation.write(out, (IndexChunkOperation) object);
			}
		}
		catch(IOException e)
		{
			throw new StorageException(e);
		}
	}

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			write(buff, obj[i]);
		}
	}

	@Override
	public TransactionOperation read(ByteBuffer buf)
	{
		try
		{
			BinaryDataInput in = BinaryDataInput.forBuffer(buf);

			int type = in.read();
			switch(type)
			{
				case MessageConstants.START_TRANSACTION:
					return StartOperation.read(in);
				case MessageConstants.COMMIT_TRANSACTION:
					return CommitOperation.read(in);
				case MessageConstants.ROLLBACK_TRANSACTION:
					return RollbackOperation.read(in);
				case MessageConstants.DELETE:
					return DeleteOperation.read(in);
				case MessageConstants.STORE_CHUNK:
					return StoreChunkOperation.read(in);
				case MessageConstants.INDEX_CHUNK:
					return IndexChunkOperation.read(in);
				default:
					throw new IOException("Unknown type of operation");
			}
		}
		catch(IOException e)
		{
			throw new StorageException(e);
		}
	}

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
	{
		for(int i=0; i<len; i++)
		{
			obj[i] = read(buff);
		}
	}
}
