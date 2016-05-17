package se.l4.silo.engine.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;

public class TokenFieldType
	implements SearchFieldType
{
	private static final Analyzer TOKEN_ANALYZER = new KeywordAnalyzer();
	
	@Override
	public boolean isLanguageSpecific()
	{
		return false;
	}
	
	@Override
	public FieldType getDefaultFieldType()
	{
		return StringField.TYPE_NOT_STORED;
	}
	
	@Override
	public Analyzer getAnalyzer(Language lang)
	{
		return TOKEN_ANALYZER;
	}
	
	@Override
	public IndexableField create(
			String field,
			FieldType type, 
			Language lang,
			Object object)
	{
		return new Field(field, object == null ? null : object.toString(), type);
	}
	
	@Override
	public IndexableField createValuesField(String field, Language lang, Object object)
	{
		return new SortedSetDocValuesField(field, new BytesRef(object.toString()));
	}
	
	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		return new SortedDocValuesField(field, new BytesRef(object.toString()));
	}
	
	@Override
	public SortField createSortField(String field, boolean ascending)
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
		return new TermQuery(new Term(field, value.toString()));
	}
	
	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		throw new UnsupportedOperationException("Token fields do not support range queries; Internal field name was " + field);
	}
}
