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
import se.l4.silo.index.search.facets.FacetQuery;

public abstract class SearchIndexQueryImpl<T, FR extends SearchResult<T>>
	implements SearchIndexQuery<T, FR>
{
	private final String name;
	private final Locale locale;
	private final ImmutableList<QueryClause> clauses;
	private final ImmutableList<FieldSort> sortOrder;
	private final ImmutableList<FacetQuery> facets;

	public SearchIndexQueryImpl(
		String name,
		Locale locale,
		ImmutableList<QueryClause> clauses,
		ImmutableList<FieldSort> sortOrder,
		ImmutableList<FacetQuery> facets
	)
	{
		this.name = name;
		this.locale = locale;
		this.clauses = clauses;
		this.sortOrder = sortOrder;
		this.facets = facets;
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

	@Override
	public ListIterable<FacetQuery> getFacets()
	{
		return facets;
	}

	public static <T> Builder<T> create(String name, Class<T> type)
	{
		return new BuilderImpl<>(
			name,
			null,
			Lists.immutable.empty(),
			Lists.immutable.empty(),
			Lists.immutable.empty()
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
			ImmutableList<FacetQuery> facets,
			OptionalLong resultOffset,
			OptionalLong resultLimit
		)
		{
			super(name, locale, clauses, sortOrder, facets);

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
		protected final ImmutableList<FacetQuery> facets;

		public BaseBuilderImpl(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			ImmutableList<FacetQuery> facets
		)
		{
			this.name = name;
			this.locale = locale;
			this.clauses = clauses;
			this.sortOrder = sortOrder;
			this.facets = facets;
		}

		protected abstract Self create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			ImmutableList<FacetQuery> facets
		);

		@Override
		public Self withLocale(Locale locale)
		{
			return create(
				name,
				locale,
				clauses,
				sortOrder,
				facets
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
				facets
			);
		}

		@Override
		public Self addFacet(FacetQuery facet)
		{
			return create(
				name,
				locale,
				clauses,
				sortOrder,
				facets.newWith(facet)
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
				facets
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
			ImmutableList<FacetQuery> facets,
			OptionalLong resultOffset,
			OptionalLong resultLimit
		)
		{
			super(name, locale, clauses, sortOrder, facets);

			this.resultOffset = resultOffset;
			this.resultLimit = resultLimit;
		}

		@Override
		protected LimitableBuilder<T> create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			ImmutableList<FacetQuery> facets
		)
		{
			return new LimitableBuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				facets,
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
				facets,
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
				facets,
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
				facets,
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
			ImmutableList<FacetQuery> facets
		)
		{
			super(name, locale, clauses, sortOrder, facets);
		}

		@Override
		protected Builder<T> create(
			String name,
			Locale locale,
			ImmutableList<QueryClause> clauses,
			ImmutableList<FieldSort> sortOrder,
			ImmutableList<FacetQuery> facets
		)
		{
			return new BuilderImpl<>(
				name,
				locale,
				clauses,
				sortOrder,
				facets
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
				facets,
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
				facets,
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
				facets,
				OptionalLong.empty(),
				OptionalLong.empty()
			);
		}
	}
}
