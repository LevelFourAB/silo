package se.l4.silo.engine.index.search.facets;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.DocIdSet;

import se.l4.silo.engine.index.search.SearchFieldDefinition;

/**
 * Encounter used when collecting facets.
 */
public interface FacetCollectionEncounter<V>
{
	/**
	 * Get the field name used to store values for the given field.
	 *
	 * @param field
	 * @return
	 */
	String getFieldName(SearchFieldDefinition<?> field);

	/**
	 * Get the reader.
	 *
	 * @return
	 */
	LeafReader getReader();

	/**
	 * Get the matching docs.
	 *
	 * @return
	 */
	DocIdSet getDocs();

	/**
	 * Collect a value.
	 *
	 * @param value
	 */
	void collect(V value);
}
