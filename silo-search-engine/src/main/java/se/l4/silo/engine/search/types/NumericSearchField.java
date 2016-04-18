package se.l4.silo.engine.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;

/**
 * Abstract base for field types for numeric values.
 * 
 * @author Andreas Holstenson
 *
 */
public abstract class NumericSearchField
	implements SearchFieldType
{
	private static final Analyzer ANALYZER = new KeywordAnalyzer();
	
	private final FieldType type;
	
	public NumericSearchField()
	{
		FieldType ft = new FieldType();
	    ft.setStored(false);
	    ft.setTokenized(true);
	    ft.setOmitNorms(true);
	    ft.setIndexOptions(IndexOptions.DOCS);
	    ft.freeze();
	    
	    this.type = ft;
	}
	
	@Override
	public boolean isLanguageSpecific()
	{
		return false;
	}

	@Override
	public FieldType getDefaultFieldType()
	{
		return type;
	}

	@Override
	public Analyzer getAnalyzer(Language lang)
	{
		return ANALYZER;
	}

	@Override
	public IndexableField create(
			String field,
			FieldType type,
			Language lang,
			Object object)
	{
		return create(field, (Number) object, type);
	}
	
	protected abstract IndexableField create(String field, 
			Number number,
			FieldType type);

	@Override
	public Object extract(IndexableField field)
	{
		return field.numericValue();
	}
	
	
}
