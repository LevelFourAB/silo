package se.l4.silo.engine.search.facets;

import java.io.IOException;
import java.util.List;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.IndexDefinitionEncounter;
import se.l4.silo.search.FacetEntry;
import se.l4.silo.search.SearchIndexQuery;

/**
 * Faceting support for {@link SearchIndexQuery}.
 * 
 * @author Andreas Holstenson
 *
 * @param <QueryParams>
 */
public interface Facet<QueryParams>
{
	/**
	 * Get the type of this facet.
	 * 
	 * @return
	 */
	String type();
	
	/**
	 * Enhance the current {@link IndexDefinition} with extra information needed
	 * for this facet to function.
	 * 
	 * @param encounter
	 */
	void setup(IndexDefinitionEncounter encounter);
	
	/**
	 * Collect the facets for the given encounter.
	 * 
	 * @param encounter
	 * @return
	 * @throws IOException
	 */
	List<FacetEntry> collect(FacetCollectionEncounter<QueryParams> encounter)
		throws IOException;
}
