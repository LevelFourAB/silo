package se.l4.silo.engine.search.internal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import se.l4.silo.StorageException;
import se.l4.silo.engine.search.FacetDefinition;
import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.IndexDefinitionEncounter;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchIndexConfig;
import se.l4.silo.engine.search.facets.Facet;

public class IndexDefinitionImpl
	implements IndexDefinition
{
	private final Locale locale;
	private final String languageField;
	private final Map<String, FieldDefinition> fields;
	private final Map<String, FacetDefinition> facets;
	private final Set<String> valueFields;

	public IndexDefinitionImpl(SearchEngine engine, SearchIndexConfig config)
	{
		locale = engine.getDefaultLanguage();
		languageField = config.getLanguageField();
		
		ImmutableMap.Builder<String, FieldDefinition> fields = ImmutableMap.builder();
		for(SearchIndexConfig.FieldConfig fc : config.getFields())
		{
			fields.put(fc.getName(), new FieldDefinitionImpl(fc));
		}
		
		this.fields = fields.build();
		
		IndexDefinitionEncounterImpl encounter = new IndexDefinitionEncounterImpl(this.fields);
		
		ImmutableMap.Builder<String, FacetDefinition> facets = ImmutableMap.builder();
		for(Map.Entry<String, Facet<?>> f : config.getFacets().entrySet())
		{
			String id = f.getKey();
			Facet<?> instance = f.getValue();
			instance.setup(encounter);
			facets.put(id, new FacetDefinitionImpl(id, instance));
		}
		this.facets = facets.build();
		
		this.valueFields = ImmutableSet.copyOf(encounter.valueFields);
	}
	
	@Override
	public Locale getDefaultLanguage()
	{
		return locale;
	}
	
	@Override
	public String getLanguageField()
	{
		return languageField;
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
	
	@Override
	public FacetDefinition getFacet(String facetId)
	{
		return facets.get(facetId);
	}
	
	@Override
	public Set<String> getValueFields()
	{
		return valueFields;
	}
	
	private static class IndexDefinitionEncounterImpl
		implements IndexDefinitionEncounter
	{
		private final Set<String> valueFields;
		private final Map<String, FieldDefinition> currentFields;
		
		public IndexDefinitionEncounterImpl(Map<String, FieldDefinition> currentFields)
		{
			this.currentFields = currentFields;
			valueFields = new HashSet<>();
		}
		
		@Override
		public void addValuesField(String field)
		{
			if(! currentFields.containsKey(field))
			{
				throw new StorageException("The field `" + field + "` has not been defined");
			}
			
			valueFields.add(field);
		}
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
		public String sortValuesName(Locale language)
		{
			return name('s', getName(), language);
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
			String fieldName = name('s', name, language == null ? null : language.getLocale());
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
	
	private static class FacetDefinitionImpl
		implements FacetDefinition
	{
		private final String id;
		private final Facet<?> instance;

		public FacetDefinitionImpl(String id, Facet<?> instance)
		{
			this.id = id;
			this.instance = instance;
		}
		
		@Override
		public String getId()
		{
			return id;
		}
		
		@Override
		public Facet<?> getInstance()
		{
			return instance;
		}
	}
}