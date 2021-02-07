package se.l4.silo.index.search.internal;

import java.util.OptionalInt;

import se.l4.silo.index.search.facets.ValueFacetQuery;

/**
 * Implementation of {@link ValueFacetQuery}.
 */
public class ValueFacetQueryImpl<T>
	implements ValueFacetQuery<T>
{
	private final String id;
	private final Class<T> valueType;
	private final OptionalInt limit;

	public ValueFacetQueryImpl(
		String id,
		Class<T> valueType,
		OptionalInt limit
	)
	{
		this.id = id;
		this.valueType = valueType;
		this.limit = limit;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Class<T> getValueType()
	{
		return valueType;
	}

	@Override
	public OptionalInt getLimit()
	{
		return limit;
	}

	public static <V> BuilderImpl<V> create(String id, Class<V> valueType)
	{
		return new BuilderImpl<>(id, valueType, OptionalInt.empty());
	}

	public static class BuilderImpl<V>
		implements Builder<V>
	{
		private final String id;
		private final Class<V> valueType;
		private final OptionalInt limit;

		public BuilderImpl(
			String id,
			Class<V> valueType,
			OptionalInt limit
		)
		{
			this.id = id;
			this.valueType = valueType;
			this.limit = limit;
		}

		@Override
		public String getId()
		{
			return id;
		}

		@Override
		public Class<V> getValueType()
		{
			return valueType;
		}

		@Override
		public Builder<V> withLimit(int topN)
		{
			if(topN <= 0)
			{
				throw new IllegalArgumentException("topN can not be less than one");
			}

			return new BuilderImpl<>(
				id,
				valueType,
				OptionalInt.of(topN)
			);
		}

		@Override
		public ValueFacetQuery<V> build()
		{
			return new ValueFacetQueryImpl<>(id, valueType, limit);
		}
	}
}
