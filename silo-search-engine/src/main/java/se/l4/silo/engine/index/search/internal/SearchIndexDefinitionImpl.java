package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import se.l4.silo.StorageException;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexEngineCreationEncounter;
import se.l4.silo.engine.index.search.Locales;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexDefinition;
import se.l4.silo.engine.index.search.config.IndexCacheConfig;
import se.l4.silo.engine.index.search.config.IndexCommitConfig;
import se.l4.silo.engine.index.search.config.IndexFreshnessConfig;
import se.l4.silo.engine.index.search.config.IndexReloadConfig;

public class SearchIndexDefinitionImpl<T>
	implements SearchIndexDefinition<T>
{
	private final String name;
	private final Locales locales;
	private final Function<T, Locale> localeSupplier;
	private final ImmutableMap<String, SearchFieldDefinition<T>> fields;

	public SearchIndexDefinitionImpl(
		String name,
		Locales locales,
		Function<T, Locale> localeSupplier,
		ImmutableMap<String, SearchFieldDefinition<T>> fields
	)
	{
		this.name = name;
		this.locales = locales;
		this.localeSupplier = localeSupplier;
		this.fields = fields;
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
	public RichIterable<SearchFieldDefinition<T>> getFields()
	{
		return fields;
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
				new IndexCommitConfig(),
				new IndexReloadConfig(
					IndexCacheConfig.create().build(),
					new IndexFreshnessConfig()
				),
				localeSupplier,
				fields
			);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to setup search index; " + e.getMessage(), e);
		}
	}

	public static <T> Builder<T> create(String name, Class<T> type)
	{
		return new BuilderImpl<>(
			name,
			LocalesImpl.DEFAULT,
			o -> Locale.ENGLISH,
			Maps.immutable.empty()
		);
	}

	public static class BuilderImpl<T>
		implements Builder<T>
	{
		private final String name;
		private final Locales locales;
		private final Function<T, Locale> localeSupplier;
		private final ImmutableMap<String, SearchFieldDefinition<T>> fields;

		public BuilderImpl(
			String name,
			Locales locales,
			Function<T, Locale> localeSupplier,
			ImmutableMap<String, SearchFieldDefinition<T>> fields
		)
		{
			this.name = name;
			this.locales = locales;
			this.localeSupplier = localeSupplier;
			this.fields = fields;
		}

		@Override
		public Builder<T> withLocale(Locale locale)
		{
			return withLocaleSupplier(o -> locale);
		}

		@Override
		public Builder<T> withLocaleSupplier(Function<T, Locale> supplier)
		{
			return new BuilderImpl<>(
				name,
				locales,
				supplier,
				fields
			);
		}

		@Override
		public Builder<T> withLocales(Locales locales)
		{
			return new BuilderImpl<>(
				name,
				locales,
				localeSupplier,
				fields
			);
		}

		@Override
		public Builder<T> addField(SearchFieldDefinition<T> field)
		{
			return new BuilderImpl<>(
				name,
				locales,
				localeSupplier,
				fields.newWithKeyValue(field.getName(), field)
			);
		}

		@Override
		public SearchIndexDefinition<T> build()
		{
			return new SearchIndexDefinitionImpl<>(
				name,
				locales,
				localeSupplier,
				fields
			);
		}
	}
}
