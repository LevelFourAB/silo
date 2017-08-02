package se.l4.silo.engine.search;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

public class TextField
	implements SearchFieldType
{
	private final boolean suggest;

	public TextField(boolean suggest)
	{
		this.suggest = suggest;
	}
	
	@Override
	public boolean isLanguageSpecific()
	{
		return true;
	}

	@Override
	public FieldType getDefaultFieldType()
	{
		return org.apache.lucene.document.TextField.TYPE_NOT_STORED;
	}

	@Override
	public Analyzer getAnalyzer(Language lang)
	{
		return lang.getTextAnalyzer();
	}
	
	@Override
	public Analyzer getSuggestAnalyzer(Language lang)
	{
		return lang.getSuggestAnalyzer();
	}

	@Override
	public IndexableField create( 
			String field,
			FieldType type, 
			Language lang, 
			Object object)
	{
		Field f;
		if(object instanceof Reader)
		{
			f = new Field(field, (Reader) object, type);
		}
		else if(object instanceof String)
		{
			f = new Field(field, (String) object, type);
		}
		else
		{
			throw new IllegalArgumentException("Text fields can not handle data of type: " + object.getClass());
		}
		
		Analyzer analyzer = suggest ? getSuggestAnalyzer(lang) : getAnalyzer(lang);
		return analyzer == null ? f : new AnalyzerField(f, analyzer);
	}
	
	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		return new SortedDocValuesField(field, new BytesRef(object.toString()));
	}
	
	@Override
	public SortField createSortField(String field, boolean ascending, Object params)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public Object extract(IndexableField field)
	{
		return field.stringValue();
	}
	
	@Override
	public Query createEqualsQuery(String field, Object value)
	{
		throw new UnsupportedOperationException("equals not supported for text fields");
	}
	
	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		throw new UnsupportedOperationException("range query not supported for text fields");
	}
}
