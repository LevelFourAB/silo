package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.ArrayUtil;
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

/**
 * Field type for indexing binary data.
 */
public final class BinaryFieldType
	implements SearchFieldType.Facetable<byte[]>
{
	public static final BinaryFieldType INSTANCE = new BinaryFieldType();

	private static final FieldType INDEX_TYPE = createFieldType();

	protected static FieldType createFieldType()
	{
		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setIndexOptions(IndexOptions.DOCS);
		ft.setTokenized(false);
		ft.freeze();
		return ft;
	}

	@Override
	public byte[] read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readByteArray();
	}

	@Override
	public void write(byte[] instance, StreamingOutput out)
		throws IOException
	{
		out.writeByteArray(instance);
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
		return false;
	}

	@Override
	public void create(FieldCreationEncounter<byte[]> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new Field(
				encounter.name(),
				encounter.getValue(),
				INDEX_TYPE
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedSetDocValuesField(
				encounter.docValuesName(),
				new BytesRef(encounter.getValue())
			));
		}
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		String field,
		Matcher<byte[]> matcher
	)
	{
		if(matcher instanceof EqualsMatcher)
		{
			byte[] data = ((EqualsMatcher<byte[]>) matcher).getValue();
			return new TermQuery(new Term(field, new BytesRef(data)));
		}

		throw new SearchIndexException("Binary fields only support equality matching");
	}

	@Override
	public FacetCollector<byte[]> createFacetCollector(
		SearchFieldDef<?> field
	)
	{
		return encounter -> {
			String fieldName = encounter.getFieldName(field);

			SortedSetDocValues values = encounter.getReader()
				.getSortedSetDocValues(fieldName);

			if(values == null) return;

			MutableLongObjectMap<byte[]> cachedOrds = LongObjectMaps.mutable.empty();

			DocIdSetIterator it = encounter.getDocs().iterator();
			int doc;
			while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
			{
				if(values.advanceExact(doc))
				{
					long value;
					while((value = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS)
					{
						byte[] byteValue = cachedOrds.get(value);
						if(byteValue == null)
						{
							BytesRef ref = values.lookupOrd(value);
							byteValue = ArrayUtil.copyOfSubArray(ref.bytes, ref.offset, ref.offset + ref.length);
							cachedOrds.put(value, byteValue);
						}

						encounter.collect(byteValue);
					}
				}
			};
		};
	}
}
