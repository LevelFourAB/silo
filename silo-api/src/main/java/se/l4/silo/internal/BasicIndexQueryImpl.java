package se.l4.silo.internal;

import java.util.OptionalLong;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.FieldSort;
import se.l4.silo.index.FieldSortBuilder;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.basic.BasicFieldLimitBuilder;
import se.l4.silo.index.basic.BasicIndexQuery;

public class BasicIndexQueryImpl<T>
	implements BasicIndexQuery<T>
{
	private final String index;
	private final ImmutableList<FieldLimit> limits;
	private final ImmutableList<FieldSort> sorting;
	private final boolean defaultSortAscending;
	private final OptionalLong resultOffset;
	private final OptionalLong resultLimit;

	public BasicIndexQueryImpl(
		String index,
		ImmutableList<FieldLimit> limits,
		ImmutableList<FieldSort> sorting,
		boolean defaultSortAscending,
		OptionalLong resultOffset,
		OptionalLong resultLimit
	)
	{
		this.index = index;
		this.limits = limits;
		this.sorting = sorting;
		this.defaultSortAscending = defaultSortAscending;
		this.resultOffset = resultOffset;
		this.resultLimit = resultLimit;
	}

	@Override
	public String getIndex()
	{
		return index;
	}

	@Override
	public ListIterable<FieldLimit> getLimits()
	{
		return limits;
	}

	@Override
	public ListIterable<FieldSort> getSortOrder()
	{
		return sorting;
	}

	@Override
	public boolean isAscendingDefaultSort()
	{
		return defaultSortAscending;
	}

	@Override
	public OptionalLong getResultLimit()
	{
		return resultLimit;
	}

	@Override
	public OptionalLong getResultOffset()
	{
		return resultOffset;
	}

	public static <T> Builder<T> create(String index)
	{
		return new BuilderImpl<>(
			index,
			Lists.immutable.empty(),
			Lists.immutable.empty(),
			true,
			OptionalLong.empty(),
			OptionalLong.empty()
		);
	}

	public static class BuilderImpl<T>
		implements Builder<T>
	{
		private final String index;
		private final ImmutableList<FieldLimit> limits;
		private final ImmutableList<FieldSort> sorting;
		private final boolean defaultSortAscending;
		private final OptionalLong resultOffset;
		private final OptionalLong resultLimit;

		public BuilderImpl(
			String index,
			ImmutableList<FieldLimit> limits,
			ImmutableList<FieldSort> sorting,
			boolean defaultSortAscending,
			OptionalLong resultOffset,
			OptionalLong resultLimit
		)
		{
			this.index = index;
			this.limits = limits;
			this.sorting = sorting;
			this.defaultSortAscending = defaultSortAscending;
			this.resultOffset = resultOffset;
			this.resultLimit = resultLimit;
		}

		@Override
		public Builder<T> add(FieldLimit limit)
		{
			return new BuilderImpl<>(
				index,
				limits.newWith(limit),
				sorting,
				defaultSortAscending,
				resultOffset,
				resultLimit
			);
		}

		@Override
		public BasicFieldLimitBuilder<Builder<T>, Object> field(String name)
		{
			return matcher -> field(name, matcher);
		}

		@Override
		public Builder<T> field(String name, Matcher matcher)
		{
			return add(FieldLimit.create(name, matcher));
		}

		@Override
		public FieldSortBuilder<Builder<T>> sort(String name)
		{
			return FieldSortBuilder.create(name, this::sort);
		}

		@Override
		public Builder<T> sort(FieldSort sort)
		{
			return new BuilderImpl<>(
				index,
				limits,
				sorting.newWith(sort),
				defaultSortAscending,
				resultOffset,
				resultLimit
			);
		}

		@Override
		public Builder<T> defaultSort(boolean ascending)
		{
			return new BuilderImpl<>(
				index,
				limits,
				sorting,
				ascending,
				resultOffset,
				resultLimit
			);
		}

		@Override
		public Builder<T> offset(long offset)
		{
			return new BuilderImpl<>(
				index,
				limits,
				sorting,
				defaultSortAscending,
				OptionalLong.of(offset),
				resultLimit
			);
		}

		@Override
		public Builder<T> limit(long limit)
		{
			return new BuilderImpl<>(
				index,
				limits,
				sorting,
				defaultSortAscending,
				resultOffset,
				OptionalLong.of(limit)
			);
		}

		@Override
		public BasicIndexQuery<T> build()
		{
			return new BasicIndexQueryImpl<>(
				index,
				limits,
				sorting,
				defaultSortAscending,
				resultOffset,
				resultLimit
			);
		}
	}
}
