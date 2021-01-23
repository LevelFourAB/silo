package se.l4.silo.engine.index.search;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import se.l4.silo.engine.index.search.internal.SearchIndex;
import se.l4.silo.engine.index.search.types.BinaryFieldType;
import se.l4.silo.engine.index.search.types.BooleanFieldType;
import se.l4.silo.engine.index.search.types.IntFieldType;
import se.l4.silo.engine.index.search.types.LocaleFieldType;
import se.l4.silo.engine.index.search.types.LongFieldType;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.engine.index.search.types.TextFieldType;
import se.l4.silo.engine.index.search.types.TokenFieldType;

/**
 * {@link SearchFieldType types} that can be used within a
 * {@link SearchIndex}.
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
	public static final SearchFieldType<String> TOKEN = new TokenFieldType();

	/**
	 * Text field.
	 */
	public static final SearchFieldType<String> TEXT = new TextFieldType(false);

	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType<Integer> INTEGER = new IntFieldType();
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType<Long> LONG = new LongFieldType();

	/**
	 * Type for booleans.
	 */
	public static final SearchFieldType<Boolean> BOOLEAN = new BooleanFieldType();

	/**
	 * Binary field.
	 */
	public static final SearchFieldType<byte[]> BINARY = new BinaryFieldType();

	/**
	 * Field suitable for storing {@link Locale}s.
	 */
	public static final SearchFieldType<Locale> LOCALE = new LocaleFieldType();

	private SearchFields()
	{
	}
}
