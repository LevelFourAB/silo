package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
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
 * {@link SearchFieldType} for indexing something as a {@link Long}.
 */
public class LongFieldType
	extends NumericFieldType<Long>
{
	@Override
	public Long read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readLong();
	}

	@Override
	public void write(Long instance, StreamingOutput out)
		throws IOException
	{
		out.writeLong(instance);
	}

	@Override
	public IndexableField create(
		String field,
		FieldType type,
		LocaleSupport lang,
		Long object
	)
	{
		return new LongPoint(field, object);
	}

	@Override
	public IndexableField createValuesField(String field, LocaleSupport lang, Long object)
	{
		return new NumericDocValuesField(field, object);
	}

	@Override
	public IndexableField createSortingField(String field, LocaleSupport lang, Long object)
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
			return LongPoint.newExactQuery(field, SearchFieldTypeHelper.toNumber(value).longValue());
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher range = (RangeMatcher) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			long lower = Long.MIN_VALUE;
			if(range.getLower().isPresent())
			{
				lower = SearchFieldTypeHelper.toNumber(range.getLower().get()).longValue();

				if(! range.isLowerInclusive())
				{
					lower = Math.addExact(lower, 1);
				}
			}

			long upper = Long.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = SearchFieldTypeHelper.toNumber(range.getUpper().get()).longValue();

				if(! range.isUpperInclusive())
				{
					upper = Math.addExact(upper, -1);
				}
			}

			return LongPoint.newRangeQuery(field, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}
}
