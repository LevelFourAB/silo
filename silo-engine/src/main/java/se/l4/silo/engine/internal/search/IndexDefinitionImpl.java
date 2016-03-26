package se.l4.silo.engine.internal.search;

import java.util.Locale;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.engine.config.SearchIndexConfig;
import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;

public class IndexDefinitionImpl
	implements IndexDefinition
{
	private final Locale locale;
	private ImmutableMap<String, FieldDefinition> fields;

	public IndexDefinitionImpl(SearchIndexConfig config)
	{
		locale = Locale.ENGLISH;
		
		ImmutableMap.Builder<String, FieldDefinition> fields = ImmutableMap.builder();
		for(SearchIndexConfig.FieldConfig fc : config.getFields())
		{
			fields.put(fc.getName(), new FieldDefinitionImpl(fc));
		}
		
		this.fields = fields.build();
	}
	
	@Override
	public Locale getDefaultLanguage()
	{
		return locale;
	}
	
	@Override
	public String getLanguageField()
	{
		return null;
	}
	
	@Override
	public FieldDefinition getField(String name)
	{
		return fields.get(name);
	}
	
	@Override
	public FieldDefinition getFieldFromIndexName(String name)
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
	public Iterable<FieldDefinition> getFields()
	{
		return fields.values();
	}
	
	private static class FieldDefinitionImpl
		implements FieldDefinition
	{
		private final SearchIndexConfig.FieldConfig fc;

		public FieldDefinitionImpl(SearchIndexConfig.FieldConfig fc)
		{
			this.fc = fc;
		}
		
		@Override
		public String getName()
		{
			return fc.getName();
		}
		
		@Override
		public SearchFieldType getType()
		{
			return fc.getType();
		}
		
		@Override
		public boolean isLanguageSpecific()
		{
			return fc.isLanguageSpecific();
		}
		
		@Override
		public boolean isMultiValued()
		{
			return fc.isMultiValued();
		}
		
		@Override
		public boolean isSorted()
		{
			return fc.isSorted();
		}
		
		@Override
		public boolean isStored()
		{
			return fc.isStored();
		}
		
		@Override
		public boolean isHighlighted()
		{
			return fc.isHighlighted();
		}
		
		private String name(char p, String field, Locale language)
		{
			return (! isLanguageSpecific() || language == null) ? p + ":" + field + ":_" : p + ":" + field + ":" + language;
		}
		
		@Override
		public String name(Locale language)
		{
			return name(getName(), language);
		}
		
		@Override
		public String docValuesName(Locale language)
		{
			return name('v', getName(), language);
		}
		
		@Override
		public String name(String field, Language language)
		{
			return name('f', field, language == null ? null : language.getLocale());
		}
		
		@Override
		public String name(String field, Locale language)
		{
			return name('f', field, language);
		}
		
		@Override
		public IndexableField createIndexableField(String name, Language language, Object data)
		{
			FieldType ft = createFieldType();
			
			ft.setStored(isStored());
			
			if(! fc.isIndexed())
			{
				ft.setIndexOptions(IndexOptions.NONE);
			}
			else
			{
				ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
			}
			
			if(fc.isHighlighted())
			{
				ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			}
			
			// TODO: Set stored/indexed values
			String fieldName = name(name, language);
			return fc.getType().create(fieldName, ft, language, data);
		}
		
		@Override
		public IndexableField createValuesField(String name, Language language, Object data)
		{
			String fieldName = name('v', name, language == null ? null : language.getLocale());
			return fc.getType().createValuesField(fieldName, language, data);
		}
		
		@Override
		public IndexableField createSortingField(String name, Language language, Object data)
		{
			String fieldName = name('v', name, language == null ? null : language.getLocale());
			return fc.getType().createSortingField(fieldName, language, data);
		}
		
		private FieldType createFieldType()
		{
			FieldType ft = new FieldType();
			FieldType defaults = fc.getType().getDefaultFieldType();
			ft.setIndexOptions(defaults.indexOptions());
			ft.setNumericPrecisionStep(defaults.numericPrecisionStep());
			ft.setNumericType(defaults.numericType());
			ft.setOmitNorms(defaults.omitNorms());
			ft.setStoreTermVectorOffsets(false);
			ft.setStoreTermVectorPositions(false);
			ft.setStoreTermVectors(false);
			ft.setTokenized(defaults.tokenized());
			return ft;
		}
	}
}
