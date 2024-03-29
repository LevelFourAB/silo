package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.search.SearchIndexException;

public class DoubleFieldType
	extends AbstractNumericFieldType<Double>
{
	public static final DoubleFieldType INSTANCE = new DoubleFieldType();

	@Override
	public Double read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readDouble();
	}

	@Override
	public void write(Double instance, StreamingOutput out)
		throws IOException
	{
		out.writeDouble(instance);
	}

	@Override
	public void create(FieldCreationEncounter<Double> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new DoublePoint(
				encounter.name(),
				encounter.getValue()
			));
		}

		if(encounter.isSorted())
		{
			encounter.emit(new DoubleDocValuesField(
				encounter.sortValuesName(),
				encounter.getValue()
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedNumericDocValuesField(
				encounter.docValuesName(),
				NumericUtils.doubleToSortableLong(encounter.getValue())
			));
		}
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.DOUBLE, ! ascending);
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		SearchFieldDef<?> fieldDef,
		Matcher<Double> matcher
	)
	{
		String fieldName = encounter.index()
			.name(fieldDef, encounter.currentLanguage());

		if(matcher instanceof EqualsMatcher)
		{
			Double value = ((EqualsMatcher<Double>) matcher).getValue();
			return DoublePoint.newExactQuery(fieldName, value);
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher<Double> range = (RangeMatcher<Double>) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			double lower = Double.NEGATIVE_INFINITY;
			if(range.getLower().isPresent())
			{
				lower = range.getLower().get();

				if(! range.isLowerInclusive())
				{
					lower = Math.nextUp(lower);
				}
			}

			double upper = Double.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = range.getUpper().get();

				if(! range.isUpperInclusive())
				{
					upper = Math.nextDown(upper);
				}
			}

			return DoublePoint.newRangeQuery(fieldName, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}
}
