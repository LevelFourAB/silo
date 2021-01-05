package se.l4.silo.index.search.internal;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.index.FieldSort;
import se.l4.silo.index.FieldSortBuilder;
import se.l4.silo.index.search.PaginatedSearchResult;
import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.SearchIndexQuery;
import se.l4.silo.index.search.SearchResult;

public abstract class SearchIndexQueryImpl<T, FR extends SearchResult<T>>
	implements SearchIndexQuery<T, FR>
{
	private final String name;
	private final Locale locale;
	private final ImmutableList<QueryClause> clauses;
	private final ImmutableList<FieldSort> sortOrder;
	private final boolean waitForLatest;

	public SearchIndexQueryImpl(
		String name,
		Locale locale,
		ImmutableList<QueryClause> clauses,
		ImmutableList<FieldSort> sortOrder,
		boolean waitForLatest
	)
	{
		this.name = name;
		this.locale = locale;
		this.clauses = clauses;
		this.sortOrder = sortOrder;
		this.waitForLatest = waitForLatest;
	}

	@Override
	public String getIndex()
	{
		return name;
	}

	@Override
	public Optional<Locale> getLocale()
	{
		return Optional.ofNullable(locale);
	}

	@Override
	public ListIterable<QueryClause> getClauses()
	{
		return clauses;
	}

	@Override
	public ListIterable<FieldSort> getSortOrder()
	{
		return sortOrder;
	}

	public static <T> Builder<T> create(String name, Class<T> type)
	{
		return new BuilderImpl<>(
			name,
			null,
			Lists.immutable.empty(),
			Lists.immutable.empty(),
			false
		);
	}

	private static class LimitedImpl<T>
		extends SearchIndexQueryImpl<T, PaginatedSearchResult<T>>
		implements Limited<T>
	{
		private final OptionalLong resultOffset;
		private final OptionalLong resultLimit;

		public LimitedImpl(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest,
			OptionalLong resultOffset,
			OptionalLong resultLimit
		)
		{
			super(name, locale, clauses, sortOrder, waitForLatest);

			this.resultOffset = resultOffset;
			this.resultLimit = resultLimit;
		}

		@Override
		public OptionalLong getResultOffset()
		{
			return resultOffset;
		}

		@Override
		public OptionalLong getResultLimit()
		{
			return resultLimit;
		}
	}

	private static abstract class BaseBuilderImpl<Self extends BaseBuilder<Self>>
		implements BaseBuilder<Self>
	{
		protected final String name;
		protected final Locale locale;
		protected final ImmutableList<QueryClause> clauses;
		protected final ImmutableList<FieldSort> sortOrder;
		protected final boolean waitForLatest;

		public BaseBuilderImpl(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest
		)
		{
			this.name = name;
			this.locale = locale;
			this.clauses = clauses;
			this.sortOrder = sortOrder;
			this.waitForLatest = waitForLatest;
		}

		protected abstract Self create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest
		);

		@Override
		public Self withLocale(Locale locale)
		{
			return create(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest
			);
		}

		@Override
		public Self add(Iterable<? extends QueryClause> clauses)
		{
			return create(
				name,
				locale,
				this.clauses.newWithAll(clauses),
				sortOrder,
				waitForLatest
			);
		}

		@Override
		public FieldSortBuilder<Self> sort(String name)
		{
			return FieldSortBuilder.create(name, this::sort);
		}

		@Override
		public Self sort(FieldSort sort)
		{
			return create(
				name,
				locale,
				clauses,
				sortOrder.newWith(sort),
				waitForLatest
			);
		}
	}

	private static class LimitableBuilderImpl<T>
		extends BaseBuilderImpl<LimitableBuilder<T>>
		implements LimitableBuilder<T>
	{
		private final OptionalLong resultOffset;
		private final OptionalLong resultLimit;

		public LimitableBuilderImpl(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest,
			OptionalLong resultOffset,
			OptionalLong resultLimit
		)
		{
			super(name, locale, clauses, sortOrder, waitForLatest);

			this.resultOffset = resultOffset;
			this.resultLimit = resultLimit;
		}

		@Override
		protected LimitableBuilder<T> create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest
		)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				resultOffset,
				resultLimit
			);
		}

		@Override
		public LimitableBuilder<T> offset(long offset)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				OptionalLong.of(offset),
				resultLimit
			);
		}

		@Override
		public LimitableBuilder<T> limit(long limit)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				resultOffset,
				OptionalLong.of(limit)
			);
		}

		@Override
		public Limited<T> build()
		{
			return new LimitedImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				resultOffset,
				resultLimit
			);
		}
	}

	private static class BuilderImpl<T>
		extends BaseBuilderImpl<Builder<T>>
		implements Builder<T>
	{
		public BuilderImpl(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest
		)
		{
			super(name, locale, clauses, sortOrder, waitForLatest);
		}

		@Override
		protected Builder<T> create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			boolean waitForLatest
		)
		{
			return new BuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest
			);
		}

		@Override
		public LimitableBuilder<T> offset(long offset)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				OptionalLong.of(offset),
				OptionalLong.empty()
			);
		}

		@Override
		public LimitableBuilder<T> limit(long limit)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				OptionalLong.empty(),
				OptionalLong.of(limit)
			);
		}

		@Override
		public LimitableBuilder<T> limited()
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				waitForLatest,
				OptionalLong.empty(),
				OptionalLong.empty()
			);
		}
	}
}
