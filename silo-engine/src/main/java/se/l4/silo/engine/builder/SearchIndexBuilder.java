package se.l4.silo.engine.builder;

import se.l4.silo.engine.search.builder.FieldBuilder;
import se.l4.silo.engine.search.facets.FacetBuilderFactory;

public interface SearchIndexBuilder<Parent>
	extends BuilderWithParent<Parent>
{
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
}
