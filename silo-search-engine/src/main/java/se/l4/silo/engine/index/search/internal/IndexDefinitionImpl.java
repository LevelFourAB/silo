package se.l4.silo.engine.index.search.internal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import se.l4.silo.engine.index.search.LocaleSupport;
import se.l4.silo.engine.index.search.Locales;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexEncounter;

public class IndexDefinitionImpl
	implements SearchIndexEncounter
{
	private final MapIterable<String, SearchFieldDefinition<?>> fields;
	private final ImmutableSet<String> valueFields;
	private final Analyzer analyzer;

	public IndexDefinitionImpl(
		Locales locales,
		MapIterable<String, SearchFieldDefinition<?>> fields
	)
	{
		this.fields = fields;

		this.valueFields = Sets.immutable.empty();

		this.analyzer = new StandardAnalyzer();
	}

	public Analyzer getAnalyzer()
	{
		return analyzer;
	}

	@Override
	public SearchFieldDefinition<?> getField(String name)
	{
		return fields.get(name);
	}

	@Override
	public SearchFieldDefinition<?> getFieldFromIndexName(String name)
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
	public RichIterable<SearchFieldDefinition<?>> getFields()
	{
		return fields;
	}

	@Override
	public SetIterable<String> getValueFields()
	{
		return valueFields;
	}

	private String name(SearchFieldDefinition<?> field, LocaleSupport localeSupport, char p)
	{
		return (! field.isLanguageSpecific() || localeSupport == null) ? p + ":" + field.getName() + ":_" : p + ":" + field.getName() + ":" + localeSupport.getLocale().toLanguageTag();
	}

	@Override
	public String name(
		SearchFieldDefinition<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 'f');
	}

	@Override
	public String docValuesName(
		SearchFieldDefinition<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 'v');
	}

	@Override
	public String sortValuesName(
		SearchFieldDefinition<?> field,
		LocaleSupport localeSupport
	)
	{
		return name(field, localeSupport, 's');
	}

	@Override
	public String nullName(SearchFieldDefinition<?> field)
	{
		return name(field, null, 'n');
	}
}
