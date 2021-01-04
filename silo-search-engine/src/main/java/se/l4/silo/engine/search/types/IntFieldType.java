package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.query.RangeMatcher;
import se.l4.silo.search.SearchIndexException;

/**
 * {@link SearchFieldType} for indexing something as an {@link Integer}.
 */
public class IntFieldType
	extends NumericFieldType<Integer>
{
	@Override
	public Integer read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readInt();
	}

	@Override
	public void write(Integer instance, StreamingOutput out)
		throws IOException
	{
		out.writeInt(instance);
	}

	@Override
	public void create(FieldCreationEncounter<Integer> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new IntPoint(
				encounter.name(),
				encounter.getValue()
			));
		}

		if(encounter.isSorted())
		{
			encounter.emit(new NumericDocValuesField(
				encounter.sortValuesName(),
				encounter.getValue()
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new NumericDocValuesField(
				encounter.docValuesName(),
				encounter.getValue()
			));
		}
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.LONG, ! ascending);
	}

	@Override
	public Query createQuery(String field, Matcher matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Object value = ((EqualsMatcher) matcher).getValue();
			return IntPoint.newExactQuery(field, toNumber(value).intValue());
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher range = (RangeMatcher) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			int lower = Integer.MIN_VALUE;
			if(range.getLower().isPresent())
			{
				lower = toNumber(range.getLower().get()).intValue();

				if(! range.isLowerInclusive())
				{
					lower = Math.addExact(lower, 1);
				}
			}

			int upper = Integer.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = toNumber(range.getUpper().get()).intValue();

				if(! range.isUpperInclusive())
				{
					upper = Math.addExact(upper, -1);
				}
			}

			return IntPoint.newRangeQuery(field, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}
}
