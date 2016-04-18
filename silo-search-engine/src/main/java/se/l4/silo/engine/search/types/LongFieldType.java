package se.l4.silo.engine.search.types;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.silo.engine.search.Language;

public class LongFieldType
	extends NumericSearchField
{
	@Override
	protected IndexableField create(String field, Number number,
			FieldType type)
	{
		return new LongPoint(field, number.longValue());
	}
	
	@Override
	public IndexableField createValuesField(String field, Language lang, Object object)
	{
		return new NumericDocValuesField(field, toLong(object));
	}
	
	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		return new NumericDocValuesField(field, toLong(object));
	}
	
	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.LONG, ! ascending);
	}
	
	@Override
	public Query createEqualsQuery(String field, Object value)
	{
		return LongPoint.newExactQuery(field, toLong(value));
	}
	
	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		return LongPoint.newRangeQuery(field, toLong(from), toLong(to));
	}
	
	private long toLong(Object value)
	{
		if(value instanceof Number)
		{
			return ((Number) value).longValue();
		}
		
		return Long.parseLong(value.toString());
	}
}
