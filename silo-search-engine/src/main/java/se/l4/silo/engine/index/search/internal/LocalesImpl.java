package se.l4.silo.engine.index.search.internal;

import java.util.Optional;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import se.l4.silo.engine.index.search.locales.EnglishLocaleSupport;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.locales.Locales;

public class LocalesImpl
	implements Locales
{
	public static final LocalesImpl DEFAULT = new LocalesImpl(
		EnglishLocaleSupport.INSTANCE,
		Maps.immutable.of(
			"en", EnglishLocaleSupport.INSTANCE
		)
	);

	private final LocaleSupport defaultInstance;
	private final ImmutableMap<String, LocaleSupport> instances;

	public LocalesImpl(
		LocaleSupport defaultInstance,
		ImmutableMap<String, LocaleSupport> instances
	)
	{
		this.defaultInstance = defaultInstance;
		this.instances = instances;
	}

	@Override
	public LocaleSupport getDefault()
	{
		return defaultInstance;
	}

	@Override
	public Optional<LocaleSupport> get(String id)
	{
		return Optional.ofNullable(instances.get(id));
	}

	@Override
	public LocaleSupport getOrDefault(String id)
	{
		return instances.getIfAbsentValue(id, defaultInstance);
	}

	public static Builder create()
	{
		return new BuilderImpl(
			DEFAULT.defaultInstance,
			DEFAULT.instances
		);
	}

	public static class BuilderImpl
		implements Builder
	{
		private final LocaleSupport defaultInstance;
		private final ImmutableMap<String, LocaleSupport> instances;

		public BuilderImpl(
			LocaleSupport defaultInstance,
			ImmutableMap<String, LocaleSupport> instances
		)
		{
			this.defaultInstance = defaultInstance;
			this.instances = instances;
		}

		@Override
		public Builder withDefault(LocaleSupport defaultInstance)
		{
			return new BuilderImpl(
				defaultInstance,
				instances
			);
		}

		@Override
		public Builder add(LocaleSupport instance)
		{
			return new BuilderImpl(
				defaultInstance,
				instances.newWithKeyValue(instance.getLocale().getLanguage(), instance)
			);
		}

		@Override
		public Locales build()
		{
			return new LocalesImpl(defaultInstance, instances);
		}
	}
}
