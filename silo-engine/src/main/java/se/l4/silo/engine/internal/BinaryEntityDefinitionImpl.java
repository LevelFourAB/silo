package se.l4.silo.engine.internal;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.Blob;
import se.l4.silo.engine.BinaryEntityDefinition;
import se.l4.silo.engine.BlobCodec;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

public class BinaryEntityDefinitionImpl<ID>
	implements BinaryEntityDefinition<ID>
{
	private final String name;
	private final TypeRef idType;
	private final TypeRef objectType;
	private final BlobCodec<ID> codec;
	private final ImmutableList<IndexDefinition<Blob<ID>>> indexes;

	public BinaryEntityDefinitionImpl(
		String name,
		TypeRef idType,
		ImmutableList<IndexDefinition<Blob<ID>>> indexes
	)
	{
		this.name = name;
		this.idType = idType;
		this.objectType = Types.reference(Blob.class).withTypeParameter(0, idType).get();
		this.indexes = indexes;
		this.codec = new BlobCodec<>();
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
	public EntityCodec<Blob<ID>> getCodec()
	{
		return codec;
	}

	@Override
	public Function<Blob<ID>, ID> getIdSupplier()
	{
		return b -> b.getId();
	}

	@Override
	public ListIterable<IndexDefinition<Blob<ID>>> getIndexes()
	{
		return indexes;
	}

	public static <ID> BinaryEntityDefinition.Builder<ID> create(String name, Class<ID> idType)
	{
		Objects.requireNonNull(name, "name must be specified");
		Objects.requireNonNull(idType, "idType must be specified");
		// TODO: Validate id type
		return new BuilderImpl<>(name, Types.reference(idType), Lists.immutable.empty());
	}

	public static class BuilderImpl<ID>
		implements BinaryEntityDefinition.Builder<ID>
	{
		private final String name;
		private final TypeRef idType;
		private final ImmutableList<IndexDefinition<Blob<ID>>> indexes;

		public BuilderImpl(
			String name,
			TypeRef idType,
			ImmutableList<IndexDefinition<Blob<ID>>> indexes
		)
		{
			this.name = name;
			this.idType = idType;
			this.indexes = indexes;
		}


		@Override
		public BinaryEntityDefinition.Builder<ID> addIndex(IndexDefinition<Blob<ID>> definition)
		{
			return new BuilderImpl<>(
				name,
				idType,
				indexes.newWith(definition)
			);
		}

		@Override
		public BinaryEntityDefinition<ID> build()
		{
			return new BinaryEntityDefinitionImpl<>(
				name,
				idType,
				indexes
			);
		}
	}
}
