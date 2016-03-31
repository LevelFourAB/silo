package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

/**
 * Interface for pluggable query parsing.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryParser<T>
{
	/**
	 * Get the id of this query parser.
	 * 
	 * @return
	 */
	String id();
	
	/**
	 * Parse a query.
	 * 
	 * @param encounter
	 * @return
	 * @throws IOException
	 */
	Query parse(QueryParseEncounter<T> encounter)
		throws IOException;
}
