package se.l4.silo.engine.internal.index.basic;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.collections.api.list.ListIterable;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexDataGenerator;
import se.l4.silo.engine.index.IndexDataUpdater;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.types.ArrayFieldType;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.MergedFieldType;
import se.l4.silo.index.basic.BasicIndexQuery;

/**
 * {@link Index} that creates a queryable index similar in functionality
 * to traditional databases.
 */
public class BasicIndex<T>
	implements Index<T, BasicIndexQuery<T>>
{
	private final String name;

	private final MVStoreManager store;

	private final BasicIndexDataGenerator<T> generator;
	private final BasicIndexUpdater updater;
	private final BasicIndexQueryRunner<T> queryRunner;

	public BasicIndex(
		MVStoreManager store,
		String name,
		String uniqueName,
		ListIterable<BasicFieldDefinition<T>> fields,
		ListIterable<BasicFieldDefinition<T>> sortFields
	)
	{
		this.name = name;
		this.store = store;

		Logger logger = LoggerFactory.getLogger(BasicIndex.class.getName() + "[" + uniqueName + "]");

		BasicFieldDefinition<T>[] fieldArray = fields.toArray(new BasicFieldDefinition[fields.size()]);
		BasicFieldDefinition<T>[] sortFieldArray = sortFields.toArray(new BasicFieldDefinition[sortFields.size()]);

		MergedFieldType dataFieldType = createMultiFieldType(fields, false);
		MVMap<Long, Object[]> indexedData = store.openMap("data:" + uniqueName, LongFieldType.INSTANCE, dataFieldType);

		MergedFieldType indexKey = createFieldType(fields, true);
		MergedFieldType indexData = createFieldType(sortFields, false);

		MVMap<Object[], Object[]> index = store.openMap("index:" + uniqueName, indexKey, indexData);

		generator = new BasicIndexDataGenerator<>(
			fieldArray,
			sortFieldArray,
			dataFieldType,
			indexData
		);

		updater = new BasicIndexUpdater(
			logger,
			store,
			uniqueName,
			dataFieldType,
			indexedData,

			indexData,
			index
		);

		queryRunner = new BasicIndexQueryRunner<>(
			logger,
			store,
			fieldArray,
			sortFieldArray,
			index
		);

		if(logger.isDebugEnabled())
		{
			logger.debug(uniqueName + ": fields=" + Arrays.toString(fieldArray) + ", sortFields=" + Arrays.toString(sortFieldArray));
		}
	}

	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Create a {@link MergedFieldType} for the given fields.
	 *
	 * @param uniqueName
	 * @param fields
	 * @param fieldNames
	 * @param appendId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static <T> MergedFieldType createFieldType(
		ListIterable<BasicFieldDefinition<T>> fields,
		boolean appendId
	)
	{
		FieldType[] result = new FieldType[fields.size() + (appendId ? 1 : 0)];
		for(int i=0, n=fields.size(); i<n; i++)
		{
			BasicFieldDefinition field = fields.get(i);
			result[i] = field.getType();
		}

		if(appendId)
		{
			result[fields.size()] = LongFieldType.INSTANCE;
		}

		return new MergedFieldType(result);
	}

	/**
	 * Create a {@link MergedFieldType} that allows multiple values for
	 * every field.
	 *
	 * @param uniqueName
	 * @param fields
	 * @param fieldNames
	 * @param appendId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static <T> MergedFieldType createMultiFieldType(
		ListIterable<BasicFieldDefinition<T>> fields,
		boolean appendId
	)
	{
		FieldType[] result = new FieldType[fields.size() + (appendId ? 1 : 0)];
		for(int i=0, n=fields.size(); i<n; i++)
		{
			BasicFieldDefinition field = fields.get(i);
			result[i] = new ArrayFieldType(field.getType());
		}

		if(appendId)
		{
			result[fields.size()] = LongFieldType.INSTANCE;
		}

		return new MergedFieldType(result);
	}

	@Override
	public void close()
		throws IOException
	{
		store.close();
	}

	@Override
	public IndexDataGenerator<T> getDataGenerator()
	{
		return generator;
	}

	@Override
	public IndexDataUpdater getDataUpdater()
	{
		return updater;
	}

	@Override
	public IndexQueryRunner<T, BasicIndexQuery<T>> getQueryRunner()
	{
		return queryRunner;
	}
}
