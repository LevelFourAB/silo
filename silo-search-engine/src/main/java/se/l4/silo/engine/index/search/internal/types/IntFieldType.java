package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.IntPoint;
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
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.RangeMatcher;
import se.l4.silo.index.search.SearchIndexException;

/**
 * {@link SearchFieldType} for indexing something as an {@link Integer}.
 */
public class IntFieldType
	extends AbstractNumericFieldType<Integer>
	implements SearchFieldType.Facetable<Integer>
{
	public static final IntFieldType INSTANCE = new IntFieldType();

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
	public Query createQuery(String field, Matcher<Integer> matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Integer value = ((EqualsMatcher<Integer>) matcher).getValue();
			return IntPoint.newExactQuery(field, value);
		}
		else if(matcher instanceof RangeMatcher)
		{
			RangeMatcher<Integer> range = (RangeMatcher<Integer>) matcher;
			if(! range.getLower().isPresent() && ! range.getUpper().isPresent())
			{
				throw new SearchIndexException("Ranges without lower and upper bound are not supported");
			}

			int lower = Integer.MIN_VALUE;
			if(range.getLower().isPresent())
			{
				lower = range.getLower().get();

				if(! range.isLowerInclusive())
				{
					lower = Math.addExact(lower, 1);
				}
			}

			int upper = Integer.MAX_VALUE;
			if(range.getUpper().isPresent())
			{
				upper = range.getUpper().get();

				if(! range.isUpperInclusive())
				{
					upper = Math.addExact(upper, -1);
				}
			}

			return IntPoint.newRangeQuery(field, lower, upper);
		}

		throw new SearchIndexException("Unsupported matcher: " + matcher);
	}

	@Override
	public FacetCollector<Integer> createFacetCollector(
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
						encounter.collect((int) value);
					}
				}
			};
		};
	}
}
