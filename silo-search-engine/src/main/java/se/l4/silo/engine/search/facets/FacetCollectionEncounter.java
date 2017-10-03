package se.l4.silo.engine.search.facets;

import java.util.Locale;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.search.SearchIndexQuery;

/**
 * Encounter for when a {@link Facet} is being asked to collect facets
 * for a query.
 *
 * @author Andreas Holstenson
 *
 */
public interface FacetCollectionEncounter<QueryParams>
{
	/**
	 * Get the locale this is for.
	 *
	 * @return
	 */
	Locale getLocale();

	/**
	 * Get how the index has been defined.
	 *
	 * @return
	 */
	IndexDefinition getIndexDefinition();

	/**
	 * Get the parameters that were given in the {@link SearchIndexQuery}.
	 *
	 * @return
	 */
	QueryParams getQueryParameters();

	/**
	 * Get the index reader in use.
	 *
	 * @return
	 */
	IndexReader getIndexReader();

	/**
	 * Get the current index searcher.
	 *
	 * @return
	 */
	IndexSearcher getIndexSearcher();

	/**
	 * Get the instance of {@link FacetsCollector} in use.
	 *
	 * @return
	 */
	FacetsCollector getCollector();
}
