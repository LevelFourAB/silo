package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchIndexEncounter;
import se.l4.silo.search.QueryClause;

public interface QueryEncounter<T extends QueryClause>
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
	LocaleSupport currentLanguage();

	/**
	 * Get the default language.
	 *
	 * @return
	 */
	LocaleSupport defaultLanguage();

	/**
	 * Get the data to parse.
	 *
	 * @return
	 */
	T clause();

	/**
	 * Get the index definition.
	 *
	 * @return
	 */
	SearchIndexEncounter index();

	/**
	 * Parse another clause.
	 *
	 * @param item
	 * @return
	 * @throws IOException
	 */
	Query parse(QueryClause item)
		throws IOException;
}
