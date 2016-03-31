package se.l4.silo.engine.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;

import se.l4.silo.engine.SearchIndex;
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
	 * Get the analyzer to use for the given language.
	 * 
	 * @param language
	 * @return
	 */
	default Analyzer getSuggestAnalyzer(Language lang)
	{
		return getAnalyzer(lang);
	}
	
	/**
	 * Get the type of sorting to use for this field.
	 * @return
	 */
	SortField.Type getSortType();
	
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
	
	default IndexableField createValuesField(String field, Language lang, Object object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support values and can not be used for things such as faceting and scoring");
	}
	
	default IndexableField createSortingField(String field, Language lang, Object object)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support scoring");
	}
}
