package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.search.SearchIndexException;

/**
 * {@link SearchFieldType} for indexing something as a {@link Long}.
 */
public class LongFieldType
	extends AbstractNumericFieldType<Long>
	implements SearchFieldType.Facetable<Long>
{
	public static final LongFieldType INSTANCE = new LongFieldType();

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
	public void create(FieldCreationEncounter<Long> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new LongPoint(
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
			encounter.emit(new SortedNumericDocValuesField(
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
	public Query createQuery(
		QueryEncounter<?> encounter,
		SearchFieldDef<?> fieldDef,
		Matcher<Long> matcher
	)
	{
		String fieldName = encounter.index()
			.name(fieldDef, encounter.currentLanguage());

		if(matcher instanceof EqualsMatcher)
		{
			Long value = ((EqualsMatcher<Long>) matcher).getValue();
			return LongPoint.newExactQuery(fieldName, value);
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher<Long> range = (RangeMatcher<Long>) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			long lower = Long.MIN_VALUE;
			if(range.getLower().isPresent())
			{
				lower = range.getLower().get();

				if(! range.isLowerInclusive())
				{
					lower = Math.addExact(lower, 1);
				}
			}

			long upper = Long.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = range.getUpper().get();

				if(! range.isUpperInclusive())
				{
					upper = Math.addExact(upper, -1);
				}
			}

			return LongPoint.newRangeQuery(fieldName, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}

	@Override
	public FacetCollector<Long> createFacetCollector(
		SearchFieldDef<?> field
	)
	{
		return encounter -> {
			String fieldName = encounter.getFieldName(field);

			SortedNumericDocValues values = encounter.getReader()
				.getSortedNumericDocValues(fieldName);

			if(values == null) return;

			DocIdSetIterator it = encounter.getDocs().iterator();
			int doc;
			while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
			{
				if(values.advanceExact(doc))
				{
					long value;
					while((value = values.nextValue()) != SortedSetDocValues.NO_MORE_ORDS)
					{
						encounter.collect(value);
					}
				}
			};
		};
	}
}
