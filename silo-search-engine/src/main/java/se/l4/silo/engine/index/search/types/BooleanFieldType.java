package se.l4.silo.engine.index.search.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRefBuilder;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;

public class BooleanFieldType
	implements SearchFieldType<Boolean>
{
	private static final FieldType INDEX_TYPE = createFieldType();

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
		return false;
	}

	@Override
	public boolean isSortingSupported()
	{
		return false;
	}

	@Override
	public void create(FieldCreationEncounter<Boolean> encounter)
	{
		if(encounter.isIndexed())
		{
			byte[] data =  new byte[] { (byte) (encounter.getValue() ? 1 : 0) };
			encounter.emit(new Field(
				encounter.name(),
				data,
				INDEX_TYPE
			));
		}
	}

	@Override
	public Query createQuery(String field, Matcher<Boolean> matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Boolean value = ((EqualsMatcher<Boolean>) matcher).getValue();
			BytesRefBuilder bytesRef = new BytesRefBuilder();
			bytesRef.append((byte) (((Boolean) value).booleanValue() ? 1 : 0));
			return new TermQuery(new Term(field, bytesRef.get()));
		}

		throw new SearchIndexException("Boolean field queries require a " + EqualsMatcher.class.getName());
	}
}
