package se.l4.silo.engine.internal.tx;

import java.io.IOException;

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
import se.l4.silo.engine.types.FieldType;

public class TransactionOperationType
	implements FieldType<TransactionOperation>
{
	@Override
	public int compare(TransactionOperation o1, TransactionOperation o2)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int estimateMemory(TransactionOperation instance)
	{
		return instance.estimateMemory();
	}

	@Override
	public TransactionOperation convert(Object in)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(TransactionOperation object, BinaryDataOutput out)
		throws IOException
	{
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

	@Override
	public TransactionOperation read(BinaryDataInput in)
		throws IOException
	{
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

	@Override
	public TransactionOperation nextDown(TransactionOperation in)
	{
		throw new UnsupportedOperationException("TX ops can not be compared");
	}

	@Override
	public TransactionOperation nextUp(TransactionOperation in)
	{
		throw new UnsupportedOperationException("TX ops can not be compared");
	}
}
