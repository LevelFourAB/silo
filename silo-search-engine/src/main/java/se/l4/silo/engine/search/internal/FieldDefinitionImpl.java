package se.l4.silo.engine.search.internal;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.engine.search.types.SearchFieldType;

public class FieldDefinitionImpl<T>
	implements SearchFieldDefinition<T>
{
	private final String name;
	private final boolean isLanguageSpecific;
	private final boolean isIndexed;
	private final boolean isHighlighted;
	private final SearchFieldType<?> type;

	public FieldDefinitionImpl(
		String name,
		SearchFieldType<?> type,
		boolean isLanguageSpecific,
		boolean isIndexed,
		boolean isHighlighted
	)
	{
		this.name = name;
		this.type = type;
		this.isLanguageSpecific = isLanguageSpecific;
		this.isIndexed = isIndexed;
		this.isHighlighted = isHighlighted;
	}

	@Override
	public String getName()
	{
		return name;
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
	public SearchFieldType<?> getType()
	{
		return type;
	}

	public static class SingleImpl<T, F>
		extends FieldDefinitionImpl<T>
		implements Single<T, F>
	{
		private final Function<T, F> supplier;
		private final boolean isSorted;

		public SingleImpl(
			String name,
			SearchFieldType<?> type,
			boolean isLanguageSpecific,
			boolean isIndexed,
			boolean isHighlighted,
			Function<T, F> supplier,
			boolean isSorted
		)
		{
			super(name, type, isLanguageSpecific, isIndexed, isHighlighted);

			this.supplier = supplier;
			this.isSorted = isSorted;
		}

		@Override
		public boolean isSorted()
		{
			return isSorted;
		}

		@Override
		public Function<T, F> getSupplier()
		{
			return supplier;
		}
	}

	public static class CollectionImpl<T, F>
		extends FieldDefinitionImpl<T>
		implements Collection<T, F>
	{
		private final Function<T, Iterable<F>> supplier;

		public CollectionImpl(
			String name,
			SearchFieldType<?> type,
			boolean isLanguageSpecific,
			boolean isIndexed,
			boolean isHighlighted,
			Function<T, Iterable<F>> supplier
		)
		{
			super(name, type, isLanguageSpecific, isIndexed, isHighlighted);

			this.supplier = supplier;
		}

		@Override
		public Function<T, Iterable<F>> getSupplier()
		{
			return supplier;
		}
	}

	public static <T> Builder<T, Void> create(String name, Class<T> type)
	{
		return new BuilderImpl<>(name, null, false, true, false);
	}

	private static class BuilderImpl<T, F>
		implements Builder<T, F>
	{
		private final String name;
		private final SearchFieldType<F> type;
		private final boolean languageSpecific;
		private final boolean indexed;
		private final boolean highlighted;

		public BuilderImpl(
			String name,
			SearchFieldType<F> type,
			boolean languageSpecific,
			boolean indexed,
			boolean highlighted
		)
		{
			this.name = name;
			this.type = type;
			this.languageSpecific = languageSpecific;
			this.indexed = indexed;
			this.highlighted = highlighted;
		}

		@Override
		public <NF> Builder<T, NF> withType(SearchFieldType<NF> type)
		{

			return new BuilderImpl<>(
				name,
				type,
				type.isLocaleSupported(),
				indexed,
				highlighted
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public SingleBuilder<T, F> withSupplier(Function<T, F> supplier)
		{
			return new SingleBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				false
			);
		}

		@Override
		public CollectionBuilder<T, F> collection()
		{
			return new CollectionBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				null
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
				languageSpecific,
				indexed,
				highlighted
			);
		}

		@Override
		public Builder<T, F> withLanguageSpecific(boolean languageSpecific)
		{
			return new BuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted
			);
		}
	}

	public static class SingleBuilderImpl<T, F>
		implements SingleBuilder<T, F>
	{
		private final String name;
		private final SearchFieldType<F> type;
		private final boolean languageSpecific;
		private final boolean indexed;
		private final boolean highlighted;

		private final Function<T, F> supplier;
		private final boolean sorted;

		public SingleBuilderImpl(
			String name,
			SearchFieldType<F> type,
			boolean languageSpecific,
			boolean indexed,
			boolean highlighted,

			Function<T, F> supplier,
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
		@SuppressWarnings("unchecked")
		public SingleBuilder<T, F> withSupplier(Function<T, F> supplier)
		{
			return new SingleBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				sorted
			);
		}

		@Override
		public SingleBuilder<T, F> withSortable(boolean sorted)
		{
			return new SingleBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				sorted
			);
		}

		@Override
		public SingleBuilder<T, F> withHighlighting(boolean highlighted)
		{
			return new SingleBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				sorted
			);
		}

		@Override
		public SingleBuilder<T, F> withLanguageSpecific(boolean languageSpecific)
		{
			return new SingleBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				sorted
			);
		}

		@Override
		public SearchFieldDefinition.Single<T, F> build()
		{
			Objects.requireNonNull(type, "a type must be specified");
			Objects.requireNonNull(supplier, "a supplier must be specified");

			return new FieldDefinitionImpl.SingleImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier,
				sorted
			);
		}
	}

	public static class CollectionBuilderImpl<T, F>
		implements CollectionBuilder<T, F>
	{
		private final String name;
		private final SearchFieldType<F> type;
		private final boolean languageSpecific;
		private final boolean indexed;
		private final boolean highlighted;

		private final Function<T, Iterable<F>> supplier;

		public CollectionBuilderImpl(
			String name,
			SearchFieldType<F> type,
			boolean languageSpecific,
			boolean indexed,
			boolean highlighted,

			Function<T, Iterable<F>> supplier
		)
		{
			this.name = name;
			this.type = type;
			this.supplier = supplier;
			this.languageSpecific = languageSpecific;
			this.indexed = indexed;
			this.highlighted = highlighted;
		}

		@Override
		@SuppressWarnings("unchecked")
		public CollectionBuilder<T, F> withSupplier(Function<T, Iterable<F>> supplier)
		{
			return new CollectionBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier
			);
		}

		@Override
		public CollectionBuilder<T, F> withHighlighting(boolean highlighted)
		{
			return new CollectionBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier
			);
		}

		@Override
		public CollectionBuilder<T, F> withLanguageSpecific(boolean languageSpecific)
		{
			return new CollectionBuilderImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier
			);
		}

		@Override
		public SearchFieldDefinition.Collection<T, F> build()
		{
			Objects.requireNonNull(type, "a type must be specified");
			Objects.requireNonNull(supplier, "a supplier must be specified");

			return new FieldDefinitionImpl.CollectionImpl<>(
				name,
				type,
				languageSpecific,
				indexed,
				highlighted,

				supplier
			);
		}
	}
}
