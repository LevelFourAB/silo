package se.l4.silo.engine.index.search.internal;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;

import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.engine.index.search.facets.FacetDef;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.index.search.SearchIndexException;

public class SearchIndexEncounterImpl<T>
	implements SearchIndexEncounter<T>
{
	private final MapIterable<String, SearchField<T, ?>> fields;
	private final MapIterable<String, FacetDef<T, ?, ?>> facets;

	public SearchIndexEncounterImpl(
		Locales locales,
		RichIterable<? extends SearchFieldDef<T>> indexedFields,
		RichIterable<? extends FacetDef<T, ?, ?>> facets
	)
	{
		this.facets = facets.toMap(FacetDef::getId, f -> f);

		MutableMap<String, SearchField<T, ?>> fields = Maps.mutable.empty();

		/*
		 * Collect the fields that should be indexed.
		 */
		for(SearchFieldDef<T> def : indexedFields)
		{
			if(fields.containsKey(def.getName()))
			{
				throw new SearchIndexException("Field with id `" + def.getName() + "` already exists");
			}

			fields.put(def.getName(), new SearchFieldImpl<>(def, true, false));
		}

		/*
		 * Go through the facets and make sure all of the fields are marked
		 * as storeDocValues.
		 */
		for(FacetDef<T, ?, ?> facet : facets)
		{
			for(SearchFieldDef<T> def : facet.getFields())
			{
				SearchField<T, ?> field = fields.get(def.getName());
				if(field == null)
				{
					field = new SearchFieldImpl<>(def, false, true);
				}
				else
				{
					if(field.getDefinition() != def)
					{
						throw new SearchIndexException("Field with id `" + def.getName() + "` was already defined with a different configuration");
					}

					field = new SearchFieldImpl<>(def, field.isIndexed(), true);
				}

				fields.put(def.getName(), field);
			}
		}

		this.fields = fields.toImmutable();
	}

	@Override
	public SearchField<T, ?> getField(String name)
	{
		return fields.get(name);
	}

	@Override
	public SearchField<T, ?> getFieldFromIndexName(String name)
	{
		name = getNameFromIndexName(name);
		return getField(name);
	}

	@Override
	public String getNameFromIndexName(String name)
	{
		if(name.startsWith("f:"))
		{
			int last = name.lastIndexOf(':');
			name = name.substring(2, last);
		}

		return name;
	}

	@Override
	public RichIterable<SearchField<T, ?>> getFields()
	{
		return fields;
	}

	public FacetDef<T, ?, ?> getFacet(String id)
	{
		return facets.get(id);
	}

	private String name(SearchFieldDef<?> field, LocaleSupport localeSupport, char p)
	{
		return (! field.isLanguageSpecific() || localeSupport == null) ? p + ":" + field.getName() + ":_" : p + ":" + field.getName() + ":" + localeSupport.getLocale().toLanguageTag();
	}

	@Override
	public String name(
		SearchFieldDef<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 'f');
	}

	@Override
	public String docValuesName(
		SearchFieldDef<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 'v');
	}

	@Override
	public String sortValuesName(
		SearchFieldDef<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 's');
	}

	@Override
	public String nullName(SearchFieldDef<?> field)
	{
		return name(field, null, 'n');
	}

	private static class SearchFieldImpl<T, V>
		implements SearchField<T, V>
	{
		private final SearchFieldDef<T> definition;
		private final boolean indexed;
		private final boolean storeDocValues;

		public SearchFieldImpl(
			SearchFieldDef<T> definition,
			boolean indexed,
			boolean storeDocValues
		)
		{
			this.definition = definition;
			this.indexed = indexed;
			this.storeDocValues = storeDocValues;
		}

		@Override
		public SearchFieldDef<T> getDefinition()
		{
			return definition;
		}

		@Override
		public boolean isIndexed()
		{
			return indexed;
		}

		@Override
		public boolean isStoreDocValues()
		{
			return storeDocValues;
		}

		@Override
		public String toString()
		{
			return "SearchField{definition=" + definition + ", indexed=" + indexed + ", storeDocValues=" + storeDocValues + "}";
		}
	}
}
