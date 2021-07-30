package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.search.SearchIndexException;

public class FloatFieldType
	extends AbstractNumericFieldType<Float>
{
	public static final FloatFieldType INSTANCE = new FloatFieldType();

	@Override
	public Float read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readFloat();
	}

	@Override
	public void write(Float instance, StreamingOutput out)
		throws IOException
	{
		out.writeFloat(instance);
	}

	@Override
	public void create(FieldCreationEncounter<Float> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new FloatPoint(
				encounter.name(),
				encounter.getValue()
			));
		}

		if(encounter.isSorted())
		{
			encounter.emit(new FloatDocValuesField(
				encounter.sortValuesName(),
				encounter.getValue()
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedNumericDocValuesField(
				encounter.docValuesName(),
				NumericUtils.floatToSortableInt(encounter.getValue())
			));
		}
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.FLOAT, ! ascending);
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		String field,
		Matcher<Float> matcher
	)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Float value = ((EqualsMatcher<Float>) matcher).getValue();
			return FloatPoint.newExactQuery(field, value);
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher<Float> range = (RangeMatcher<Float>) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			float lower = Float.NEGATIVE_INFINITY;
			if(range.getLower().isPresent())
			{
				lower = range.getLower().get();

				if(! range.isLowerInclusive())
				{
					lower = Math.nextUp(lower);
				}
			}

			float upper = Float.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = range.getUpper().get();

				if(! range.isUpperInclusive())
				{
					upper = Math.nextDown(upper);
				}
			}

			return FloatPoint.newRangeQuery(field, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}
}
