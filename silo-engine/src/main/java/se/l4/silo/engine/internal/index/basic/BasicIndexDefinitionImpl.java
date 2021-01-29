package se.l4.silo.engine.internal.index.basic;

import java.util.Objects;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.index.IndexEngineCreationEncounter;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.index.basic.BasicFieldDefinition.Single;
import se.l4.silo.engine.index.basic.BasicIndexDefinition;

public class BasicIndexDefinitionImpl<T>
	implements BasicIndexDefinition<T>
{
	private final String name;
	private final ImmutableList<BasicFieldDefinition<T, ?>> fields;
	private final ImmutableList<BasicFieldDefinition.Single<T, ?>> sortFields;

	public BasicIndexDefinitionImpl(
		String name,
		ImmutableList<BasicFieldDefinition<T, ?>> fields,
		ImmutableList<BasicFieldDefinition.Single<T, ?>> sortFields
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
	public ListIterable<BasicFieldDefinition<T, ?>> getFields()
	{
		return fields;
	}

	@Override
	public ListIterable<BasicFieldDefinition.Single<T, ?>> getSortFields()
	{
		return sortFields;
	}

	@Override
	public BasicIndex<T> create(IndexEngineCreationEncounter encounter)
	{
		MVStoreManager store = encounter.openStorageWideMVStore("index");
		return new BasicIndex<>(
			store,
			encounter.getName(),
			encounter.getUniqueName(),
			fields,
			sortFields
		);
	}

	public static <T> Builder<T> create(String name)
	{
		return new BuilderImpl<>(
			name,
			Lists.immutable.empty(),
			Lists.immutable.empty()
		);
	}

	public static class BuilderImpl<T>
		implements Builder<T>
	{
		private final String name;
		private final ImmutableList<BasicFieldDefinition<T, ?>> fields;
		private final ImmutableList<BasicFieldDefinition.Single<T, ?>> sortFields;

		public BuilderImpl(
			String name,
			ImmutableList<BasicFieldDefinition<T, ?>> fields,
			ImmutableList<BasicFieldDefinition.Single<T, ?>> sortFields
		)
		{
			this.name = name;
			this.fields = fields;
			this.sortFields = sortFields;
		}

		@Override
		public Builder<T> addField(BasicFieldDefinition<T, ?> field)
		{
			Objects.requireNonNull(field);

			return new BuilderImpl<>(
				name,
				fields.newWith(field),
				sortFields
			);
		}

		@Override
		public Builder<T> addField(
			Buildable<? extends BasicFieldDefinition<T, ?>> buildable
		)
		{
			return addField(buildable.build());
		}

		@Override
		public Builder<T> addSort(BasicFieldDefinition.Single<T, ?> field)
		{
			return new BuilderImpl<>(
				name,
				fields,
				sortFields.newWith(field)
			);
		}

		@Override
		public Builder<T> addSort(Buildable<? extends Single<T, ?>> buildable)
		{
			return addSort(buildable.build());
		}

		@Override
		public BasicIndexDefinition<T> build()
		{
			return new BasicIndexDefinitionImpl<>(name, fields, sortFields);
		}
	}
}
