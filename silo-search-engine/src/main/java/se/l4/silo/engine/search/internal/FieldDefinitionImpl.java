package se.l4.silo.engine.search.internal;

import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.engine.search.SearchFieldType;

public class FieldDefinitionImpl<T>
	implements SearchFieldDefinition<T>
{
	private final String name;
	private final Function<T, Object> supplier;
	private final boolean isLanguageSpecific;
	private final boolean isIndexed;
	private final boolean isStored;
	private final boolean isHighlighted;
	private final boolean isSorted;
	private final boolean isStoreValues;
	private final SearchFieldType<?> type;

	public FieldDefinitionImpl(
		String name,
		SearchFieldType<?> type,
		Function<T, Object> supplier,
		boolean isLanguageSpecific,
		boolean isIndexed,
		boolean isStored,
		boolean isHighlighted,
		boolean isSorted,
		boolean isStoreValues
	)
	{
		this.name = name;
		this.type = type;
		this.supplier = supplier;
		this.isLanguageSpecific = isLanguageSpecific;
		this.isIndexed = isIndexed;
		this.isStored = isStored;
		this.isHighlighted = isHighlighted;
		this.isSorted = isSorted;
		this.isStoreValues = isStoreValues;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Function<T, Object> getSupplier()
	{
		return supplier;
	}

	@Override
	public boolean isLanguageSpecific()
	{
		return isLanguageSpecific;
	}

	@Override
	public boolean isIndexed()
	{
		return isIndexed;
	}


	@Override
	public boolean isHighlighted()
	{
		return isHighlighted;
	}

	@Override
	public boolean isSorted()
	{
		return isSorted;
	}

	@Override
	public SearchFieldType<?> getType()
	{
		return type;
	}

	public static <T> Builder<T, Void> create(String name, Class<T> type)
	{
		return new BuilderImpl<>(name, null, null, false, true, false, false);
	}

	private static class BuilderImpl<T, F>
		implements Builder<T, F>
	{
		private final String name;
		private final SearchFieldType<?> type;
		private final Function<T, Object> supplier;
		private final boolean languageSpecific;
		private final boolean indexed;
		private final boolean highlighted;
		private final boolean sorted;

		public BuilderImpl(
			String name,
			SearchFieldType<?> type,
			Function<T, Object> supplier,
			boolean languageSpecific,
			boolean indexed,
			boolean highlighted,
			boolean sorted
		)
		{
			this.name = name;
			this.type = type;
			this.supplier = supplier;
			this.languageSpecific = languageSpecific;
			this.indexed = indexed;
			this.highlighted = highlighted;
			this.sorted = sorted;
		}

		@Override
		public <NF> Builder<T, NF> withType(SearchFieldType<NF> type)
		{
			if(supplier != null)
			{
				throw new StorageException("Supplier provided before type was set");
			}

			return new BuilderImpl<>(
				name,
				type,
				null,
				type.isLanguageSpecific(),
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Builder<T, F> withSupplier(Function<T, F> supplier)
		{
			return new BuilderImpl<>(
				name,
				type,
				(Function<T, Object>) supplier,
				languageSpecific,
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		public Builder<T, Iterable<F>> collection()
		{
			if(supplier != null)
			{
				throw new StorageException("Supplier provided before type was set");
			}

			return new BuilderImpl<>(
				name,
				type,
				null,
				languageSpecific,
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		public Builder<T, F> sortable()
		{
			return withSortable(true);
		}

		@Override
		public Builder<T, F> withSortable(boolean sorted)
		{
			return new BuilderImpl<>(
				name,
				type,
				supplier,
				languageSpecific,
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		public Builder<T, F> withHighlighting()
		{
			return withHighlighting(true);
		}

		@Override
		public Builder<T, F> withHighlighting(boolean highlighted)
		{
			return new BuilderImpl<>(
				name,
				type,
				supplier,
				languageSpecific,
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		public Builder<T, F> withLanguageSpecific(boolean languageSpecific)
		{
			return new BuilderImpl<>(
				name,
				type,
				supplier,
				languageSpecific,
				indexed,
				highlighted,
				sorted
			);
		}

		@Override
		public SearchFieldDefinition<T> build()
		{
			return new FieldDefinitionImpl<>(
				name,
				type,
				supplier,
				languageSpecific,
				indexed,
				false, // TODO
				highlighted,
				sorted,
				false // TODO
			);
		}
	}
}
