package se.l4.silo.engine.search.internal;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.search.CustomFieldCreator;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchIndexBuilder;
import se.l4.silo.engine.search.SearchIndexConfig;
import se.l4.silo.engine.search.SearchIndexConfig.FieldConfig;
import se.l4.silo.engine.search.builder.FieldBuilder;
import se.l4.silo.engine.search.facets.FacetBuilderFactory;
import se.l4.silo.engine.search.scoring.ScoringProvider;

public class SearchIndexBuilderImpl<Parent>
	implements SearchIndexBuilder<Parent>
{
	private final Function<QueryEngineConfig, Parent> configReceiver;
	private final SearchIndexConfig config;
	
	public SearchIndexBuilderImpl(Function<QueryEngineConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		config = new SearchIndexConfig();
	}
	
	@Override
	public SearchIndexBuilder<Parent> setLanguageField(String name)
	{
		config.setLanguageField(name);
		return this;
	}

	@Override
	public FieldBuilder<SearchIndexBuilder<Parent>> addField(String field)
	{
		Objects.requireNonNull(field, "field name can not be null");
		
		return new FieldBuilderImpl<>(field, (c) -> {
			config.addField(c);
			return this;
		});
	}
	
	@Override
	public <T extends BuilderWithParent<SearchIndexBuilder<Parent>>> T addFacet(String facetId,
			FacetBuilderFactory<SearchIndexBuilder<Parent>, T> factory)
	{
		return factory.create(c -> {
			config.addFacet(facetId, c);
			return this;
		});
	}
	
	@Override
	public SearchIndexBuilder<Parent> addCustomFieldCreator(CustomFieldCreator creator)
	{
		config.addFieldCreator(creator);
		return this;
	}
	
	@Override
	public SearchIndexBuilder<Parent> addScoringProvider(ScoringProvider<?> provider)
	{
		config.addScoringProvider(provider.id(), provider);
		return this;
	}
	
	@Override
	public Parent done()
	{
		return configReceiver.apply(config);
	}
	
	private static class FieldBuilderImpl<Parent>
		implements FieldBuilder<Parent>
	{
		private final String name;
		private final Function<FieldConfig, Parent> configReceiver;
		
		private boolean indexed;
		private boolean stored;
		private Boolean languageSpecific;
		private boolean multiValued;
		private SearchFieldType type;
		private boolean highlighted;
		private boolean sorted;


		public FieldBuilderImpl(String name, Function<FieldConfig, Parent> configReceiver)
		{
			this.name = name;
			this.configReceiver = configReceiver;
			
			indexed = true;
		}
		
		@Override
		public FieldBuilder<Parent> indexed(boolean indexed)
		{
			this.indexed = indexed;
			
			return this;
		}
		
		@Override
		public FieldBuilder<Parent> languageSpecific()
		{
			return languageSpecific(true);
		}
		
		@Override
		public FieldBuilder<Parent> languageSpecific(boolean language)
		{
			this.languageSpecific = language;
			
			return this;
		}
		
		@Override
		public FieldBuilder<Parent> multiValued()
		{
			return multiValued(true);
		}
		
		@Override
		public FieldBuilder<Parent> multiValued(boolean multivalued)
		{
			this.multiValued = multivalued;
			
			return this;
		}
		
		@Override
		public FieldBuilder<Parent> stored()
		{
			return stored(true);
		}
		
		@Override
		public FieldBuilder<Parent> stored(boolean store)
		{
			this.stored = store;
			
			return this;
		}
		
		@Override
		public FieldBuilderImpl<Parent> type(SearchFieldType type)
		{
			Objects.requireNonNull(type, "type can not be null");
			
			this.type = type;
			if(languageSpecific == null)
			{
				languageSpecific = type.isLanguageSpecific();
			}
			
			return this;
		}
		
		@Override
		public FieldBuilder<Parent> highlighted()
		{
			return highlighted(true);
		}
		
		@Override
		public FieldBuilder<Parent> highlighted(boolean highlighted)
		{
			this.highlighted = highlighted;
			if(highlighted)
			{
				this.stored = true;
			}
			
			return this;
		}
		
		@Override
		public FieldBuilder<Parent> sorted()
		{
			return sorted(true);
		}
		
		@Override
		public FieldBuilder<Parent> sorted(boolean sorted)
		{
			this.sorted = sorted;
			return this;
		}
		
		@Override
		public Parent done()
		{
			Objects.requireNonNull(type, "type can not be null");
			if(highlighted && ! stored)
			{
				throw new IllegalArgumentException("Field can not be highlighted and not stored");
			}
			
			return configReceiver.apply(new FieldConfig(
				name, 
				type,
				languageSpecific, 
				multiValued, 
				stored, 
				indexed,
				highlighted,
				sorted
			));
		}
	}
}
