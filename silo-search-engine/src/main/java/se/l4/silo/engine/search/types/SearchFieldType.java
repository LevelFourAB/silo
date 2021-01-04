package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.query.Matcher;

/**
 * Type of data that can be used for {@link SearchFieldDefinition fields}.
 */
public interface SearchFieldType<T>
{
	/**
	 * Write the given instance to the output.
	 *
	 * @param instance
	 * @param out
	 * @throws IOException
	 */
	void write(T instance, StreamingOutput out)
		throws IOException;

	/**
	 * Read an instance from the given input.
	 *
	 * @param instance
	 * @param in
	 * @return
	 * @throws IOException
	 */
	T read(StreamingInput in)
		throws IOException;

	/**
	 * Get if this type supports different variants based on locales.
	 *
	 * @return
	 */
	boolean isLocaleSupported();

	/**
	 * Get if this type supports sorting.
	 *
	 * @return
	 */
	boolean isSortingSupported();

	/**
	 * Get if this type supports doc values.
	 *
	 * @return
	 */
	boolean isDocValuesSupported();

	/**
	 * Create a query for the given instance of {@link Matcher}.
	 *
	 * @param matcher
	 * @return
	 */
	Query createQuery(String field, Matcher matcher);

	/**
	 * Create the field from the given object.
	 *
	 * @param object
	 * @return
	 */
	void create(
		FieldCreationEncounter<T> encounter
	);

	/**
	 * Create a {@link SortField} for this type.
	 *
	 * @param field
	 * @param ascending
	 * @param params
	 * @return
	 */
	default SortField createSortField(String field, boolean ascending)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support sorting");
	}
}
