package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;

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
import se.l4.silo.index.search.SearchIndexException;

public class BooleanFieldType
	implements SearchFieldType.Facetable<Boolean>
{
	public static final BooleanFieldType INSTANCE = new BooleanFieldType();

	private static final FieldType INDEX_TYPE = createFieldType();
	private static final BytesRef TRUE = new BytesRef("T");
	private static final BytesRef FALSE = new BytesRef("F");

	protected static FieldType createFieldType()
	{
		FieldType ft = new FieldType();
		ft.setIndexOptions(IndexOptions.DOCS);
		ft.setTokenized(false);
		ft.setOmitNorms(true);
		ft.freeze();
		return ft;
	}

	@Override
	public Boolean read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readBoolean();
	}

	@Override
	public void write(Boolean instance, StreamingOutput out)
		throws IOException
	{
		out.writeBoolean(instance);
	}

	@Override
	public boolean isLocaleSupported()
	{
		return false;
	}

	@Override
	public boolean isDocValuesSupported()
	{
		return true;
	}

	@Override
	public boolean isSortingSupported()
	{
		return true;
	}

	@Override
	public void create(FieldCreationEncounter<Boolean> encounter)
	{
		BytesRef value = encounter.getValue() ? TRUE : FALSE;

		if(encounter.isIndexed())
		{
			encounter.emit(new Field(
				encounter.name(),
				value,
				INDEX_TYPE
			));
		}

		if(encounter.isSorted())
		{
			encounter.emit(new SortedDocValuesField(
				encounter.sortValuesName(),
				value
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedSetDocValuesField(
				encounter.docValuesName(),
				value
			));
		}
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		String field,
		Matcher<Boolean> matcher
	)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Boolean value = ((EqualsMatcher<Boolean>) matcher).getValue();
			return new TermQuery(new Term(field, value ? TRUE : FALSE));
		}

		throw new SearchIndexException("Boolean field queries require a " + EqualsMatcher.class.getName());
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public FacetCollector<Boolean> createFacetCollector(
		SearchFieldDef<?> field
	)
	{
		return encounter -> {
			String fieldName = encounter.getFieldName(field);

			SortedSetDocValues values = encounter.getReader()
				.getSortedSetDocValues(fieldName);

			if(values == null) return;

			MutableLongObjectMap<Boolean> cachedOrds = LongObjectMaps.mutable.empty();

			DocIdSetIterator it = encounter.getDocs().iterator();
			int doc;
			while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
			{
				if(values.advanceExact(doc))
				{
					long ord;
					while((ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS)
					{
						Boolean value = cachedOrds.get(ord);
						if(value == null)
						{
							BytesRef ref = values.lookupOrd(ord);
							value = ref.equals(TRUE);
							cachedOrds.put(ord, value);
						}

						encounter.collect(value);
					}
				}
			};
		};
	}
}
