package se.l4.silo.engine.search.types;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;

public class TextFieldType
	implements SearchFieldType
{
	private final boolean suggest;

	public TextFieldType(boolean suggest)
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
		return suggest ? lang.getSuggestAnalyzer() : lang.getTextAnalyzer();
	}

	@Override
	public IndexableField create(
			String field,
			FieldType type,
			Language lang,
			Object object)
	{
		if(object instanceof Reader)
		{
			return new Field(field, (Reader) object, type);
		}
		else if(object instanceof String)
		{
			return new Field(field, (String) object, type);
		}
		else
		{
			throw new IllegalArgumentException("Text fields can not handle data of type: " + object.getClass());
		}
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
