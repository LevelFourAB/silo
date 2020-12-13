package se.l4.silo.engine.internal.index;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngineCreationEncounter;
import se.l4.silo.engine.index.FieldDefinition;
import se.l4.silo.engine.index.FieldIndexDefinition;

public class FieldIndexDefinitionImpl<T>
	implements FieldIndexDefinition<T>
{
	private final String name;
	private final ImmutableList<FieldDefinition<T>> fields;
	private final ImmutableList<FieldDefinition<T>> sortFields;

	public FieldIndexDefinitionImpl(
		String name,
		ImmutableList<FieldDefinition<T>> fields,
		ImmutableList<FieldDefinition<T>> sortFields
	)
	{
		this.name = name;
		this.fields = fields;
		this.sortFields = sortFields;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public ListIterable<FieldDefinition<T>> getFields()
	{
		return fields;
	}

	@Override
	public ListIterable<FieldDefinition<T>> getSortFields()
	{
		return sortFields;
	}

	@Override
	public FieldIndexQueryEngine<T> create(QueryEngineCreationEncounter encounter)
	{
		MVStoreManager store = encounter.openStorageWideMVStore("index");
		return new FieldIndexQueryEngine<>(
			store,
			encounter.getName(),
			encounter.getUniqueName(),
			fields,
			sortFields
		);
	}

	public static <T> Builder<T> create(String name)
	{
		return new BuilderImpl<>(name, Lists.immutable.empty(), Lists.immutable.empty());
	}

	public static class BuilderImpl<T>
		implements Builder<T>
	{
		private final String name;
		private final ImmutableList<FieldDefinition<T>> fields;
		private final ImmutableList<FieldDefinition<T>> sortFields;

		public BuilderImpl(
			String name,
			ImmutableList<FieldDefinition<T>> fields,
			ImmutableList<FieldDefinition<T>> sortFields
		)
		{
			this.name = name;
			this.fields = fields;
			this.sortFields = sortFields;
		}

		@Override
		public Builder<T> addField(FieldDefinition<T> field)
		{
			return new BuilderImpl<>(
				name,
				fields.newWith(field),
				sortFields
			);
		}

		@Override
		public Builder<T> addSort(FieldDefinition<T> field)
		{
			return new BuilderImpl<>(
				name,
				fields,
				sortFields.newWith(field)
			);
		}

		@Override
		public FieldIndexDefinition<T> build()
		{
			return new FieldIndexDefinitionImpl<>(name, fields, sortFields);
		}
	}
}
