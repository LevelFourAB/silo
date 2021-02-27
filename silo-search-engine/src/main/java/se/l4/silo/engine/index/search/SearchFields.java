package se.l4.silo.engine.index.search;

import java.util.Locale;

import se.l4.silo.engine.index.search.internal.SearchIndex;
import se.l4.silo.engine.index.search.types.BinaryFieldType;
import se.l4.silo.engine.index.search.types.BooleanFieldType;
import se.l4.silo.engine.index.search.types.IntFieldType;
import se.l4.silo.engine.index.search.types.LocaleFieldType;
import se.l4.silo.engine.index.search.types.LongFieldType;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.engine.index.search.types.StringFieldType;
import se.l4.silo.engine.index.search.types.TextFieldType;

/**
 * {@link SearchFieldType types} that can be used within a
 * {@link SearchIndex}.
 *
 * @author Andreas Holstenson
 *
 */
public class SearchFields
{

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

	/**
	 * Start building a field type that stores {@link String strings}.
	 *
	 * @return
	 */
	public static StringFieldType.Builder string()
	{
		return StringFieldType.create();
	}
}
