package se.l4.silo.engine.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.silo.engine.search.builder.FieldBuilder;

/**
 * Information about a field that can be placed in a {@link SearchIndex}.
 *
 * @author Andreas Holstenson
 *
 */
public interface SearchFieldType
{
	/**
	 * Get if this field normally depends on {@link Language}. This will
	 * automatically call {@link FieldBuilder#languageSpecific()} when
	 * the field is used.
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
	Analyzer getAnalyzer(Language lang);

	/**
	 * Create the field from the given object.
	 *
	 * @param object
	 * @return
	 */
	IndexableField create(
		String field,
		FieldType type,
		Language lang,
		Object object
	);

	/**
	 * Extract a value from the given field.
	 *
	 * @param field
	 * @param locale
	 * @return
	 */
	Object extract(IndexableField field);

	/**
	 * Get a query that represents the given field name matching the given
	 * value.
	 *
	 * @param value
	 * @return
	 */
	Query createEqualsQuery(String field, Object value);

	/**
	 * Create a range query for the given field name.
	 *
	 * @param field
	 * @param from
	 * @param to
	 * @return
	 */
	Query createRangeQuery(String field, Object from, Object to);

	/**
	 * Create a {@link SortField} for this type.
	 *
	 * @param field
	 * @param ascending
	 * @param params TODO
	 * @return
	 */
	default SortField createSortField(String field, boolean ascending, Object params)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support sorting");
	}

	default IndexableField createValuesField(String field, Language lang, Object object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support values and can not be used for things such as faceting and scoring");
	}

	default IndexableField createSortingField(String field, Language lang, Object object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support sorting");
	}
}
