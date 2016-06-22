package se.l4.silo.engine.search;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.search.builder.FieldBuilder;
import se.l4.silo.engine.search.facets.FacetBuilderFactory;
import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;
import se.l4.silo.engine.search.scoring.ScoringProvider;

/**
 * Builder for {@link SearchIndexQueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface SearchIndexBuilder<Parent>
	extends BuilderWithParent<Parent>
{
	/**
	 * Set the field to use for getting the language of data stored.
	 * 
	 * @param name
	 * @return
	 */
	SearchIndexBuilder<Parent> setLanguageField(String name);
	
	/**
	 * Start adding a field to this index.
	 * 
	 * @param field
	 * @return
	 */
	FieldBuilder<SearchIndexBuilder<Parent>> addField(String field);
	
	/**
	 * Start adding a new facet to this index.
	 * 
	 * @param facetId
	 * @return
	 */
	<T extends BuilderWithParent<SearchIndexBuilder<Parent>>> T addFacet(String facetId, FacetBuilderFactory<SearchIndexBuilder<Parent>, T> factory);
	
	/**
	 * Add a {@link CustomFieldCreator} to this index. This allows for 
	 * adding dynamic fields from the input data.
	 * 
	 * @param creator
	 * @return
	 */
	SearchIndexBuilder<Parent> addCustomFieldCreator(CustomFieldCreator creator);
	
	/**
	 * Add a scoring provider that can be used for this index.
	 * 
	 * @param provider
	 * @return
	 */
	SearchIndexBuilder<Parent> addScoringProvider(ScoringProvider<?> provider);
}
