package se.l4.silo.engine.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;
import se.l4.silo.engine.search.types.BinaryFieldType;
import se.l4.silo.engine.search.types.BooleanFieldType;
import se.l4.silo.engine.search.types.IntFieldType;
import se.l4.silo.engine.search.types.LongFieldType;
import se.l4.silo.engine.search.types.TokenFieldType;
import se.l4.silo.search.query.SuggestQuery;

/**
 * {@link SearchFieldType types} that can be used within a
 * {@link SearchIndexQueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 */
public class SearchFields
{
	public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();
	
	/**
	 * Token field, saved the entire input string as single token. Does not
	 * use localization and does not store the input by default.
	 */
	public static final SearchFieldType TOKEN = new TokenFieldType();
	
	/**
	 * Text field.
	 */
	public static final SearchFieldType TEXT = new TextField(false);
	
	/**
	 * Type that can be used for building fields that are suitable for
	 * use with {@link SuggestQuery}. 
	 */
	public static final SearchFieldType SUGGEST = new TextField(true);
	
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType INTEGER = new IntFieldType();
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType LONG = new LongFieldType();
	
	/**
	 * Type for booleans.
	 */
	public static final SearchFieldType BOOLEAN = new BooleanFieldType();
	
	/**
	 * Binar field.
	 */
	public static final SearchFieldType BINARY = new BinaryFieldType();

	private SearchFields()
	{
	}
}
