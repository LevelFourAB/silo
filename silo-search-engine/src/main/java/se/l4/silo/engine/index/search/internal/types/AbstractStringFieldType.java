package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.StringFieldType;

/**
 * Abstract implementation of {@link StringFieldType}.
 */
public abstract class AbstractStringFieldType
	implements StringFieldType
{
	@Override
	public String read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readString();
	}

	@Override
	public void write(String instance, StreamingOutput out)
		throws IOException
	{
		out.writeString(instance);
	}

	@Override
	public boolean isSortingSupported()
	{
		return true;
	}

	@Override
	public boolean isDocValuesSupported()
	{
		return true;
	}

	@Override
	public void create(FieldCreationEncounter<String> encounter)
	{
		if(encounter.isIndexed())
		{
			createIndexed(encounter);
		}

		if(encounter.isSorted())
		{
			encounter.emit(new SortedDocValuesField(
				encounter.sortValuesName(),
				new BytesRef(encounter.getValue())
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

	protected abstract void createIndexed(FieldCreationEncounter<String> encounter);

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public FacetCollector<String> createFacetCollector(
		SearchFieldDefinition<?> field
	)
	{
		return encounter -> {
			String fieldName = encounter.getFieldName(field);

			SortedSetDocValues values = encounter.getReader()
				.getSortedSetDocValues(fieldName);

			if(values == null) return;

			MutableLongObjectMap<String> cachedOrds = LongObjectMaps.mutable.empty();

			DocIdSetIterator it = encounter.getDocs().iterator();
			int doc;
			while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
			{
				if(values.advanceExact(doc))
				{
					long ord;
					while((ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS)
					{
						String value = cachedOrds.get(ord);
						if(value == null)
						{
							BytesRef ref = values.lookupOrd(ord);
							value = ref.utf8ToString();
							cachedOrds.put(ord, value);
						}

						encounter.collect(value);
					}
				}
			};
		};
	}
}
