package se.l4.silo.engine.internal;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.ObjectCodec;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

/**
 * Implementation of {@link CollectionDef}.
 */
public class CollectionDefImpl<ID, T>
	implements CollectionDef<ID, T>
{
	private final String name;
	private final TypeRef idType;
	private final TypeRef objectType;
	private final ObjectCodec<T> codec;
	private final Function<T, ID> idSupplier;
	private final ImmutableList<IndexDefinition<T>> indexes;

	public CollectionDefImpl(
		String name,
		TypeRef idType,
		TypeRef objectType,
		ObjectCodec<T> codec,
		Function<T, ID> idSupplier,
		ImmutableList<IndexDefinition<T>> indexes
	)
	{
		this.name = name;
		this.idType = idType;
		this.objectType = objectType;
		this.codec = codec;
		this.idSupplier = idSupplier;
		this.indexes = indexes;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public TypeRef getIdType()
	{
		return idType;
	}

	@Override
	public TypeRef getDataType()
	{
		return objectType;
	}

	@Override
	public ObjectCodec<T> getCodec()
	{
		return codec;
	}

	@Override
	public Function<T, ID> getIdSupplier()
	{
		return idSupplier;
	}

	@Override
	public ListIterable<IndexDefinition<T>> getIndexes()
	{
		return indexes;
	}

	public static <T> Builder<Void, T> create(String name, Class<T> type)
	{
		Objects.requireNonNull(name, "name must be specified");
		Objects.requireNonNull(type, "type must be specified");
		return new BuilderImpl<>(name, null, Types.reference(type), null, null, Lists.immutable.empty());
	}

	public static class BuilderImpl<ID, T>
		implements Builder<ID, T>
	{
		private final String name;
		private final TypeRef idType;
		private final TypeRef objectType;
		private final ObjectCodec<T> codec;
		private final Function<T, ID> idSupplier;
		private final ImmutableList<IndexDefinition<T>> indexes;

		public BuilderImpl(
			String name,
			TypeRef idType,
			TypeRef objectType,
			ObjectCodec<T> codec,
			Function<T, ID> idSupplier,
			ImmutableList<IndexDefinition<T>> indexes
		)
		{
			this.name = name;
			this.idType = idType;
			this.objectType = objectType;
			this.codec = codec;
			this.idSupplier = idSupplier;
			this.indexes = indexes;
		}

		@Override
		public Builder<ID, T> withCodec(ObjectCodec<T> codec)
		{
			return new BuilderImpl<>(
				name,
				idType,
				objectType,
				codec,
				idSupplier,
				indexes
			);
		}

		@Override
		public <NewID> Builder<NewID, T> withId(Class<NewID> type, Function<T, NewID> idFunction)
		{
			Class<?> unwrapped = Types.unwrap(type);
			if(unwrapped != int.class
				&& unwrapped != long.class
				&& unwrapped != String.class
				&& unwrapped != byte[].class)
			{
				throw new IllegalArgumentException("Invalid id type, only int, long, string or byte[] is supported, got: " + type);
			}

			return new BuilderImpl<>(
				name,
				Types.reference(type),
				objectType,
				codec,
				idFunction,
				indexes
			);
		}

		@Override
		public Builder<ID, T> addIndex(IndexDefinition<T> definition)
		{
			Objects.requireNonNull(definition);

			return new BuilderImpl<>(
				name,
				idType,
				objectType,
				codec,
				idSupplier,
				indexes.newWith(definition)
			);
		}

		@Override
		public Builder<ID, T> addIndex(Buildable<? extends IndexDefinition<T>> buildable)
		{
			return addIndex(buildable.build());
		}

		@Override
		public CollectionDef<ID, T> build()
		{
			Objects.requireNonNull(codec, "codec must be specified");
			Objects.requireNonNull(idSupplier, "idSupplier must be specified");

			return new CollectionDefImpl<>(name, idType, objectType, codec, idSupplier, indexes);
		}
	}
}
