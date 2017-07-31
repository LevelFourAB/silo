package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.Language;
import se.l4.silo.search.QueryItem;

public interface QueryParseEncounter<T>
{
	/**
	 * Get if the query is using a language other than the default.
	 *
	 * @return
	 */
	boolean isSpecificLanguage();

	/**
	 * Get the current language.
	 *
	 * @return
	 */
	Language currentLanguage();

	/**
	 * Get the default language.
	 *
	 * @return
	 */
	Language defaultLanguage();

	/**
	 * Get the data to parse.
	 *
	 * @return
	 */
	T data();

	/**
	 * Get the index definition.
	 *
	 * @return
	 */
	IndexDefinition def();

	/**
	 * Parse another query item.
	 *
	 * @param item
	 * @return
	 * @throws IOException
	 */
	Query parse(QueryItem item)
		throws IOException;

	/**
	 * Get this encounter but with some other data.
	 *
	 * @param data
	 * @return
	 */
	<C> QueryParseEncounter<C> withData(C data);
}
