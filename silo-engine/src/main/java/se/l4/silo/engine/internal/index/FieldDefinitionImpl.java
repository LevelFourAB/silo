package se.l4.silo.engine.internal.index;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.StorageException;
import se.l4.silo.engine.index.FieldDefinition;
import se.l4.silo.engine.types.BooleanFieldType;
import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.IntFieldType;
import se.l4.silo.engine.types.LongFieldType;
import se.l4.silo.engine.types.StringFieldType;

public class FieldDefinitionImpl<T>
	implements FieldDefinition<T>
{
	private final String name;
	private final Function<T, Object> supplier;
	private final boolean collection;
	private final FieldType<?> type;

	public FieldDefinitionImpl(
		String name,
		Function<T, Object> supplier,
		boolean collection,
		FieldType<?> type
	)
	{
		this.name = name;
		this.supplier = supplier;
		this.collection = collection;
		this.type = type;
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
	public boolean isCollection()
	{
		return collection;
	}

	@Override
	public FieldType<?> getType()
	{
		return type;
	}

	public static <T> Builder<T, Void> create(String name)
	{
		return new BuilderImpl<>(name, null, false, null);
	}

	private static class BuilderImpl<T, F>
		implements Builder<T, F>
	{
		private final String name;
		private final Function<T, Object> supplier;
		private final boolean collection;
		private final FieldType<?> type;

		public BuilderImpl(
			String name,
			Function<T, Object> supplier,
			boolean collection,
			FieldType<?> type
		)
		{
			this.name = name;
			this.supplier = supplier;
			this.collection = collection;
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
			if(supplier != null)
			{
				throw new StorageException("Supplier provided before type was set");
			}

			return new BuilderImpl<>(name, supplier, collection, type);
		}

		@Override
		public Builder<T, Iterable<F>> collection()
		{
			if(supplier != null)
			{
				throw new StorageException("Supplier provided before type was set");
			}

			return new BuilderImpl<>(name, supplier, true, type);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Builder<T, F> withSupplier(Function<T, F> supplier)
		{
			return new BuilderImpl<>(name, (Function<T, Object>) supplier, collection, type);
		}

		@Override
		public FieldDefinition<T> build()
		{
			Objects.requireNonNull(supplier, "a supplier must be present");
			Objects.requireNonNull(type, "a type must be be specified");
			return new FieldDefinitionImpl<>(name, supplier, collection, type);
		}
	}
}
