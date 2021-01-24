package se.l4.silo.engine.internal.index.basic;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.collections.api.factory.Lists;

import se.l4.silo.StorageException;
import se.l4.silo.engine.index.IndexDataGenerator;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.io.BinaryDataOutput;
import se.l4.silo.engine.types.MergedFieldType;

public class BasicIndexDataGenerator<T>
	implements IndexDataGenerator<T>
{
	private final BasicFieldDefinition<T, ?>[] fields;
	private final BasicFieldDefinition.Single<T, ?>[] sortFields;

	private final MergedFieldType keyType;
	private final MergedFieldType sortType;

	public BasicIndexDataGenerator(
		BasicFieldDefinition<T, ?>[] fields,
		BasicFieldDefinition.Single<T, ?>[] sortFields,

		MergedFieldType dataFieldType,
		MergedFieldType indexData
	)
	{
		this.fields = fields;
		this.sortFields = sortFields;

		this.keyType = dataFieldType;
		this.sortType = indexData;
	}

	@Override
	public void generate(T data, OutputStream stream)
		throws IOException
	{
		BinaryDataOutput out = BinaryDataOutput.forStream(stream);

		// Write a version tag
		out.write(0);

		Object[][] key = new Object[fields.length][];
		for(int i=0, n=key.length; i<n; i++)
		{
			BasicFieldDefinition<T, ?> field = fields[i];
			if(field instanceof BasicFieldDefinition.Single)
			{
				key[i] = new Object[] {
					((BasicFieldDefinition.Single) field).getSupplier().apply(data)
				};
			}
			else if(field instanceof BasicFieldDefinition.Collection)
			{
				key[i] = Lists.immutable.ofAll(
					((BasicFieldDefinition.Collection<T, ?>) field).getSupplier().apply(data)
				).toArray();
			}
			else
			{
				throw new StorageException("Unknown type of field " + field);
			}
		}

		Object[] sort = new Object[sortFields.length];
		for(int i=0, n=sortFields.length; i<n; i++)
		{
			sort[i] = sortFields[i].getSupplier().apply(data);
		}

		keyType.write(key, out);
		sortType.write(sort, out);
	}
}
