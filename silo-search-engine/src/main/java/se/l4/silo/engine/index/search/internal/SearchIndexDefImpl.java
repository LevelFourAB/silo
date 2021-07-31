package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import se.l4.silo.StorageException;
import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexEngineCreationEncounter;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.SearchIndexDef;
import se.l4.silo.engine.index.search.config.IndexCacheConfig;
import se.l4.silo.engine.index.search.config.IndexCommitConfig;
import se.l4.silo.engine.index.search.facets.FacetDef;
import se.l4.silo.engine.index.search.internal.query.QueryBuildersImpl;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.index.search.SearchIndexException;

public class SearchIndexDefImpl<T>
	implements SearchIndexDef<T>
{
	private final String name;
	private final Locales locales;
	private final Function<T, Locale> localeSupplier;
	private final ImmutableMap<String, SearchFieldDef<T>> fields;
	private final ImmutableMap<String, FacetDef<T, ?, ?>> facets;

	public SearchIndexDefImpl(
		String name,
		Locales locales,
		Function<T, Locale> localeSupplier,
		ImmutableMap<String, SearchFieldDef<T>> fields,
		ImmutableMap<String, FacetDef<T, ?, ?>> facets
	)
	{
		this.name = name;
		this.locales = locales;
		this.localeSupplier = localeSupplier;
		this.fields = fields;
		this.facets = facets;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Function<T, Locale> getLocaleSupplier()
	{
		return localeSupplier;
	}

	@Override
	public RichIterable<SearchFieldDef<T>> getFields()
	{
		return fields;
	}

	@Override
	public RichIterable<FacetDef<T, ?, ?>> getFacets()
	{
		return facets;
	}

	@Override
	public Index<?, ?> create(
		IndexEngineCreationEncounter encounter
	)
	{
		Path path = encounter.resolveDataFile(encounter.getUniqueName());
		try
		{
			return new SearchIndex<>(
				encounter.getScheduler(),
				encounter.getName(),
				encounter.getUniqueName(),
				path,
				locales,
				QueryBuildersImpl.DEFAULT,
				IndexCommitConfig.create().build(),
				IndexCacheConfig.create().build(),
				localeSupplier,
				fields,
				facets
			);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to setup search index; " + e.getMessage(), e);
		}
	}

	public static <T> Builder<T> create(Class<T> type, String name)
	{
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);

		return new BuilderImpl<>(
			name,
			LocalesImpl.DEFAULT,
			o -> Locale.ENGLISH,
			Maps.immutable.empty(),
			Maps.immutable.empty()
		);
	}

	public static class BuilderImpl<T>
		implements Builder<T>
	{
		private final String name;
		private final Locales locales;
		private final Function<T, Locale> localeSupplier;
		private final ImmutableMap<String, SearchFieldDef<T>> fields;
		private final ImmutableMap<String, FacetDef<T, ?, ?>> facets;

		public BuilderImpl(
			String name,
			Locales locales,
			Function<T, Locale> localeSupplier,
			ImmutableMap<String, SearchFieldDef<T>> fields,
			ImmutableMap<String, FacetDef<T, ?, ?>> facets
		)
		{
			this.name = name;
			this.locales = locales;
			this.localeSupplier = localeSupplier;
			this.fields = fields;
			this.facets = facets;
		}

		@Override
		public Builder<T> withLocale(Locale locale)
		{
			Objects.requireNonNull(locale);

			return withLocaleSupplier(o -> locale);
		}

		@Override
		public Builder<T> withLocaleSupplier(Function<T, Locale> supplier)
		{
			Objects.requireNonNull(supplier);

			return new BuilderImpl<>(
				name,
				locales,
				supplier,
				fields,
				facets
			);
		}

		@Override
		public Builder<T> withLocales(Locales locales)
		{
			Objects.requireNonNull(locales);

			return new BuilderImpl<>(
				name,
				locales,
				localeSupplier,
				fields,
				facets
			);
		}

		@Override
		public Builder<T> addField(SearchFieldDef<T> field)
		{
			Objects.requireNonNull(field);

			if(fields.containsKey(field.getName()))
			{
				throw new SearchIndexException("Field with id `" + field.getName() + "` already exists in index");
			}

			return new BuilderImpl<>(
				name,
				locales,
				localeSupplier,
				fields.newWithKeyValue(field.getName(), field),
				facets
			);
		}

		@Override
		public Builder<T> addField(
			Buildable<? extends SearchFieldDef<T>> buildable
		)
		{
			return addField(buildable.build());
		}

		@Override
		public Builder<T> addFields(
			Iterable<? extends SearchFieldDef<T>> fields
		)
		{
			Builder<T> result = this;

			for(SearchFieldDef<T> field : fields)
			{
				result = result.addField(field);
			}

			return result;
		}

		@Override
		public Builder<T> addFacet(FacetDef<T, ?, ?> facet)
		{
			Objects.requireNonNull(facet);

			if(facets.containsKey(facet.getId()))
			{
				throw new SearchIndexException("Facet with id `" + facet.getId() + "` already exists in index");
			}

			return new BuilderImpl<>(
				name,
				locales,
				localeSupplier,
				fields,
				facets.newWithKeyValue(facet.getId(), facet)
			);
		}

		@Override
		public Builder<T> addFacets(Iterable<? extends FacetDef<T, ?, ?>> facets)
		{
			Builder<T> result = this;

			for(FacetDef<T, ?, ?> facet : facets)
			{
				result = result.addFacet(facet);
			}

			return result;
		}

		@Override
		public SearchIndexDef<T> build()
		{
			return new SearchIndexDefImpl<>(
				name,
				locales,
				localeSupplier,
				fields,
				facets
			);
		}
	}
}
