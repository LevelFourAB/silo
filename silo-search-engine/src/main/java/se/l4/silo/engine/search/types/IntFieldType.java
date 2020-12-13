package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchFieldType;
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
	public IndexableField create(
		String field,
		FieldType type,
		LocaleSupport lang,
		Integer object
	)
	{
		return new IntPoint(field, object);
	}

	@Override
	public IndexableField createValuesField(String field, LocaleSupport lang, Integer object)
	{
		return new NumericDocValuesField(field, object);
	}

	@Override
	public IndexableField createSortingField(String field, LocaleSupport lang, Integer object)
	{
		return new NumericDocValuesField(field, object);
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
			return IntPoint.newExactQuery(field, SearchFieldTypeHelper.toNumber(value).intValue());
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
				lower = SearchFieldTypeHelper.toNumber(range.getLower().get()).intValue();

				if(! range.isLowerInclusive())
				{
					lower = Math.addExact(lower, 1);
				}
			}

			int upper = Integer.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = SearchFieldTypeHelper.toNumber(range.getUpper().get()).intValue();

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
