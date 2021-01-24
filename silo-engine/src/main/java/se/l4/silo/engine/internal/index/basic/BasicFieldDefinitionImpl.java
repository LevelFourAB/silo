package se.l4.silo.engine.internal.index.basic;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.types.BooleanFieldType;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.IntFieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;

public abstract class BasicFieldDefinitionImpl<T, V>
	implements BasicFieldDefinition<T, V>
{
	private final String name;
	private final FieldType<V> type;

	public BasicFieldDefinitionImpl(
		String name,
		FieldType<V> type
	)
	{
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public FieldType<V> getType()
	{
		return type;
	}

	public static <T> Builder<T, Void> create(String name)
	{
		return new BuilderImpl<>(name, null);
	}

	public static class SingleImpl<T, V>
		extends BasicFieldDefinitionImpl<T, V>
		implements Single<T, V>
	{
		private final Function<T, V> supplier;

		public SingleImpl(
			String name,
			FieldType<V> type,
			Function<T, V> supplier
		)
		{
			super(name, type);

			this.supplier = supplier;
		}

		@Override
		public Function<T, V> getSupplier()
		{
			return supplier;
		}
	}

	public static class CollectionImpl<T, V>
		extends BasicFieldDefinitionImpl<T, V>
		implements Collection<T, V>
	{
		private final Function<T, Iterable<V>> supplier;

		public CollectionImpl(
			String name,
			FieldType<V> type,
			Function<T, Iterable<V>> supplier
		)
		{
			super(name, type);

			this.supplier = supplier;
		}

		@Override
		public Function<T, Iterable<V>> getSupplier()
		{
			return supplier;
		}
	}

	private static class BuilderImpl<T, F>
		implements Builder<T, F>
	{
		private final String name;
		private final FieldType<F> type;

		public BuilderImpl(
			String name,
			FieldType<F> type
		)
		{
			this.name = name;
			this.type = type;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <NF> Builder<T, NF> withType(Class<NF> type)
		{
			if(type == int.class)
			{
				return (Builder) withType(IntFieldType.INSTANCE);
			}
			else if(type == long.class)
			{
				return (Builder) withType(LongFieldType.INSTANCE);
			}
			else if(type == String.class)
			{
				return (Builder) withType(StringFieldType.INSTANCE);
			}
			else if(type == boolean.class)
			{
				return (Builder) withType(BooleanFieldType.INSTANCE);
			}

			throw new StorageException(
				"Unsupported Java type " + type.getName() + ", supply a custom "
				+ FieldType.class.getSimpleName() + " to use with indexes"
			);
		}

		@Override
		public <NF> Builder<T, NF> withType(FieldType<NF> type)
		{
			return new BuilderImpl<>(name, type);
		}

		@Override
		public CollectionBuilder<T, F> collection()
		{
			return new CollectionBuilderImpl<>(name, type, null);
		}

		@Override
		@SuppressWarnings("unchecked")
		public SingleBuilder<T, F> withSupplier(Function<T, F> supplier)
		{
			Objects.requireNonNull(supplier);

			return new SingleBuilderImpl<>(name, type, supplier);
		}
	}

	private static class SingleBuilderImpl<T, F>
		implements SingleBuilder<T, F>
	{
		private final String name;
		private final FieldType<F> type;
		private final Function<T, F> supplier;

		public SingleBuilderImpl(
			String name,
			FieldType<F> type,
			Function<T, F> supplier
		)
		{
			this.name = name;
			this.type = type;
			this.supplier = supplier;
		}

		@Override
		public SingleBuilder<T, F> withSupplier(Function<T, F> supplier)
		{
			Objects.requireNonNull(supplier);

			return new SingleBuilderImpl<>(
				name,
				type,
				supplier
			);
		}

		@Override
		public Single<T, F> build()
		{
			Objects.requireNonNull(supplier, "supplier required");

			return new SingleImpl<>(name, type, supplier);
		}
	}

	private static class CollectionBuilderImpl<T, F>
		implements CollectionBuilder<T, F>
	{
		private final String name;
		private final FieldType<F> type;
		private final Function<T, Iterable<F>> supplier;

		public CollectionBuilderImpl(
			String name,
			FieldType<F> type,
			Function<T, Iterable<F>> supplier
		)
		{
			this.name = name;
			this.type = type;
			this.supplier = supplier;
		}

		@Override
		public CollectionBuilder<T, F> withSupplier(Function<T, Iterable<F>> supplier)
		{
			Objects.requireNonNull(supplier);

			return new CollectionBuilderImpl<>(
				name,
				type,
				supplier
			);
		}

		@Override
		public Collection<T, F> build()
		{
			Objects.requireNonNull(supplier, "supplier required");

			return new CollectionImpl<>(name, type, supplier);
		}
	}
}
