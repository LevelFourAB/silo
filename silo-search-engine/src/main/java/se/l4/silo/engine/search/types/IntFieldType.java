package se.l4.silo.engine.search.types;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.silo.engine.search.Language;

public class IntFieldType
	extends NumericFieldType
{
	@Override
	protected IndexableField create(String field, Number number, FieldType type)
	{
		return new IntPoint(field, number.intValue());
	}

	@Override
	public IndexableField createValuesField(String field, Language lang, Object object)
	{
		return new NumericDocValuesField(field, toInt(object));
	}

	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		return new NumericDocValuesField(field, toInt(object));
	}

	@Override
	public SortField createSortField(String field, boolean ascending, Object params)
	{
		return new SortField(field, SortField.Type.LONG, ! ascending);
	}

	@Override
	public Query createEqualsQuery(String field, Object value)
	{
		return IntPoint.newExactQuery(field, toInt(value));
	}

	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		return IntPoint.newRangeQuery(field, toInt(from), toInt(to));
	}

	private int toInt(Object value)
	{
		if(value instanceof Number)
		{
			return ((Number) value).intValue();
		}

		return Integer.parseInt(value.toString());
	}
}
