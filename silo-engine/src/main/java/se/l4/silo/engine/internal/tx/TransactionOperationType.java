package se.l4.silo.engine.internal.tx;

import java.io.IOException;

import com.google.common.collect.ComparisonChain;

import se.l4.commons.io.ExtendedDataInput;
import se.l4.commons.io.ExtendedDataOutput;
import se.l4.commons.serialization.Serializer;
import se.l4.silo.engine.internal.IOUtils;
import se.l4.silo.engine.internal.tx.TransactionOperation.Type;
import se.l4.silo.engine.types.FieldType;

public class TransactionOperationType
	implements FieldType<TransactionOperation>
{

	@Override
	public String uniqueId()
	{
		return "internal:tx-op";
	}

	@Override
	public int compare(TransactionOperation o1, TransactionOperation o2)
	{
		return ComparisonChain.start()
			.compare(o1.getType(), o2.getType())
			.compare(o1.getEntity(), o2.getEntity())
			.result();
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
	public Serializer<TransactionOperation> getSerializer()
	{
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void write(TransactionOperation instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(instance.getType().ordinal());
		
		switch(instance.getType())
		{
			case STORE_CHUNK:
			case DELETE:
				out.writeString(instance.getEntity());
				IOUtils.writeId(instance.getId(), out);
				break;
		}
		
		if(instance.getType() == TransactionOperation.Type.STORE_CHUNK)
		{
			IOUtils.writeByteArray(instance.getData(), out);
		}
		else if(instance.getType() == TransactionOperation.Type.START)
		{
			out.writeVLong(instance.getTimestamp());
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
			case START:
				long timestamp = in.readVLong();
				return TransactionOperation.start(timestamp);
		}
		
		return null;
	}

}
