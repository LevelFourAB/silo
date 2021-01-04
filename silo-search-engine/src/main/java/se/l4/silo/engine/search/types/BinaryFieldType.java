package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.search.SearchIndexException;

/**
 * Field type for indexing binary data.
 */
public final class BinaryFieldType
	implements SearchFieldType<byte[]>
{
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
		return false;
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
	}

	@Override
	public Query createQuery(String field, Matcher matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			byte[] data = (byte[]) ((EqualsMatcher) matcher).getValue();
			return new TermQuery(new Term(field, new BytesRef(data)));
		}

		throw new SearchIndexException("Binary fields only support equality matching");
	}
}
