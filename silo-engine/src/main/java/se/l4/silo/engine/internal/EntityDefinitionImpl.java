package se.l4.silo.engine.internal;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

public class EntityDefinitionImpl<ID, T>
	implements EntityDefinition<ID, T>
{
	private final String name;
	private final TypeRef idType;
	private final TypeRef objectType;
	private final EntityCodec<T> codec;
	private final Function<T, ID> idSupplier;
	private final ImmutableList<IndexDefinition<T>> indexes;

	public EntityDefinitionImpl(
		String name,
		TypeRef idType,
		TypeRef objectType,
		EntityCodec<T> codec,
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
	public EntityCodec<T> getCodec()
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
		private final EntityCodec<T> codec;
		private final Function<T, ID> idSupplier;
		private final ImmutableList<IndexDefinition<T>> indexes;

		public BuilderImpl(
			String name,
			TypeRef idType,
			TypeRef objectType,
			EntityCodec<T> codec,
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
		public Builder<ID, T> withCodec(EntityCodec<T> codec)
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
			// TODO: Validate type of id

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
		public EntityDefinition<ID, T> build()
		{
			Objects.requireNonNull(codec, "codec must be specified");
			Objects.requireNonNull(idSupplier, "idSupplier must be specified");

			return new EntityDefinitionImpl<>(name, idType, objectType, codec, idSupplier, indexes);
		}
	}
}
