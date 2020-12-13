package se.l4.silo.engine.search;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
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
	 * Get if this field normally depends on the language of text.
	 *
	 * @return
	 */
	boolean isLanguageSpecific();

	/**
	 * Get the default field type. This is used to allow the indexer to change
	 * certain values before indexing.
	 *
	 * @return
	 */
	FieldType getDefaultFieldType();

	/**
	 * Get the analyzer to use for the given language.
	 *
	 * @param language
	 * @return
	 */
	Analyzer getAnalyzer(LocaleSupport lang);

	/**
	 * Create the field from the given object.
	 *
	 * @param object
	 * @return
	 */
	IndexableField create(
		String field,
		FieldType type,
		LocaleSupport localeSupport,
		T object
	);

	/**
	 * Create a query for the given instance of {@link Matcher}.
	 *
	 * @param matcher
	 * @return
	 */
	Query createQuery(String field, Matcher matcher);

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

	default IndexableField createValuesField(String field, LocaleSupport lang, T object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support values and can not be used for things such as faceting and scoring");
	}

	default IndexableField createSortingField(String field, LocaleSupport lang, T object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support sorting");
	}
}
