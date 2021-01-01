package se.l4.silo.engine.internal.tx;

import java.io.IOException;

import se.l4.silo.engine.internal.IOUtils;
import se.l4.silo.engine.internal.tx.TransactionOperation.Type;
import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataOutput;
import se.l4.silo.engine.types.FieldType;

public class TransactionOperationType
	implements FieldType<TransactionOperation>
{
	@Override
	public int compare(TransactionOperation o1, TransactionOperation o2)
	{
		int c = o1.getType().compareTo(o2.getType());
		if(c != 0) return c;

		return o1.getEntity().compareTo(o2.getEntity());
	}

	@Override
	public int estimateMemory(TransactionOperation instance)
	{
		int result = 16;

		// Enum
		result += 24;

		// Entity
		if(instance.getEntity() != null)
		{
			result += 24 + 2 * instance.getEntity().length();
		}
		else
		{
			result += 8;
		}

		// Id
		if(instance.getId() instanceof Long)
		{
			result += 30;
		}
		else if(instance.getId() instanceof Integer)
		{
			result += 24;
		}
		else if(instance.getId() instanceof String)
		{
			result += 24 + 2 * instance.getId().toString().length();
		}
		else
		{
			result += 8;
		}

		// Data
		if(instance.getData() != null)
		{
			result += 64 + instance.getData().length * 4;
		}
		else
		{
			result += 8;
		}

		return result;
	}

	@Override
	public TransactionOperation convert(Object in)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(TransactionOperation instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(instance.getType().ordinal());

		switch(instance.getType())
		{
			case START:
				out.writeVLong(instance.getTimestamp());
				break;
			case STORE_CHUNK:
				out.writeString(instance.getEntity());
				IOUtils.writeId(instance.getId(), out);
				IOUtils.writeByteArray(instance.getData(), out);
				break;
			case INDEX_CHUNK:
				out.writeString(instance.getEntity());
				IOUtils.writeId(instance.getId(), out);
				IOUtils.writeByteArray(instance.getData(), out);
				break;
			case DELETE:
				out.writeString(instance.getEntity());
				IOUtils.writeId(instance.getId(), out);
				break;
		}
	}

	@Override
	public TransactionOperation read(ExtendedDataInput in)
		throws IOException
	{
		int t = in.readVInt();
		Type type = TransactionOperation.Type.values()[t];
		switch(type)
		{
			case COMMIT:
				return TransactionOperation.commit();
			case ROLLBACK:
				return TransactionOperation.rollback();
			case DELETE:
				return TransactionOperation.delete(
					in.readString(),
					IOUtils.readId(in)
				);
			case STORE_CHUNK:
				return TransactionOperation.store(
					in.readString(),
					IOUtils.readId(in),
					IOUtils.readByteArray(in)
				);
			case INDEX_CHUNK:
				String rawEntity = in.readString();
				int idx = rawEntity.lastIndexOf("::");
				return TransactionOperation.indexChunk(
					rawEntity.substring(0, idx),
					rawEntity.substring(idx + 2),
					IOUtils.readId(in),
					IOUtils.readByteArray(in)
				);
			case START:
				long timestamp = in.readVLong();
				return TransactionOperation.start(timestamp);
		}

		return null;
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
