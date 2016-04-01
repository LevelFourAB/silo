package se.l4.silo.engine.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

public abstract class NumericSearchField
	implements SearchFieldType
{
	private static final Analyzer ANALYZER = new KeywordAnalyzer();
	
	private final FieldType type;
	
	public NumericSearchField(NumericType type)
	{
		FieldType ft = new FieldType();
	    ft.setStored(false);
	    ft.setTokenized(true);
	    ft.setOmitNorms(true);
	    ft.setIndexOptions(IndexOptions.DOCS);
	    ft.setNumericType(type);
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
	
	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		// TODO Auto-generated method stub
		return SearchFieldType.super.createSortingField(field, lang, object);
	}
}
