package se.l4.silo.engine.search.internal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.Locales;
import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchIndexEncounter;

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

		this.analyzer = new DelegatingAnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY)
		{
			private final Analyzer standard = new StandardAnalyzer();

			@Override
			protected Analyzer getWrappedAnalyzer(String fieldName)
			{
				if(fieldName.startsWith("f:"))
				{
					int last = fieldName.lastIndexOf(':');
					String locale = fieldName.substring(last + 1);

					if(locale.equals("_"))
					{
						return locales.getDefault().getTextAnalyzer();
					}
					else
					{
						LocaleSupport localeSupport = locales.getOrDefault(locale);
						SearchFieldDefinition<?> def = fields.get(fieldName.substring(2, last));
						if(def == null)
						{
							return localeSupport.getTextAnalyzer();
						}
						else
						{
							return def.getType().getAnalyzer(localeSupport);
						}
					}
				}

				return standard;
			}
		};
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

	@Override
	public IndexableField createIndexableField(
		LocaleSupport localeSupport,
		SearchFieldDefinition<?> field,
		Object data
	)
	{
		FieldType ft = createFieldType(field.getType());

		//ft.setStored(field.isStored());

		if(! field.isIndexed())
		{
			ft.setIndexOptions(IndexOptions.NONE);
		}
		else
		{
			ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		}

		if(field.isHighlighted())
		{
			ft.setStored(true);
			ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		}

		// TODO: Set stored/indexed values
		String fieldName = name(field, localeSupport);
		return ((SearchFieldType) field.getType()).create(fieldName, ft, localeSupport, data);
	}

	@Override
	public IndexableField createValuesField(
		LocaleSupport localeSupport,
		SearchFieldDefinition<?> field,
		Object data
	)
	{
		String fieldName = docValuesName(field, localeSupport);
		return ((SearchFieldType) field.getType()).createValuesField(fieldName, localeSupport, data);
	}

	@Override
	public IndexableField createSortingField(
		LocaleSupport localeSupport,
		SearchFieldDefinition<?> field,
		Object data
	)
	{
		String fieldName = sortValuesName(field, localeSupport);
		return ((SearchFieldType) field.getType()).createSortingField(fieldName, localeSupport, data);
	}

	private FieldType createFieldType(SearchFieldType<?> fieldType)
	{
		FieldType ft = new FieldType();
		FieldType defaults = fieldType.getDefaultFieldType();
		ft.setIndexOptions(defaults.indexOptions());
		ft.setOmitNorms(defaults.omitNorms());
		ft.setStoreTermVectorOffsets(false);
		ft.setStoreTermVectorPositions(false);
		ft.setStoreTermVectors(false);
		ft.setTokenized(defaults.tokenized());
		return ft;
	}
}
