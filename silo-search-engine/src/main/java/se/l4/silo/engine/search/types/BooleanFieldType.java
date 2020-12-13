package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRefBuilder;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchFields;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.search.SearchIndexException;

public class BooleanFieldType
	implements SearchFieldType<Boolean>
{
	private final FieldType type = createFieldType();

	protected static FieldType createFieldType()
	{
		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setIndexOptions(IndexOptions.NONE);
		ft.setTokenized(false);
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
	public boolean isLanguageSpecific()
	{
		return false;
	}

	@Override
	public FieldType getDefaultFieldType()
	{
		return type;
	}

	@Override
	public Analyzer getAnalyzer(LocaleSupport lang)
	{
		return SearchFields.DEFAULT_ANALYZER;
	}

	@Override
	public IndexableField create(
		String field,
		FieldType type,
		LocaleSupport lang,
		Boolean object
	)
	{
		byte[] data =  new byte[] { (byte) (((Boolean) object).booleanValue() ? 1 : 0) };
		return new Field(field, data, type);
	}

	@Override
	public Query createQuery(String field, Matcher matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Object value = ((EqualsMatcher) matcher).getValue();
			if(! (value instanceof Boolean))
			{
				throw new SearchIndexException("Querying for equality requires a boolean");
			}

			BytesRefBuilder bytesRef = new BytesRefBuilder();
			bytesRef.append((byte) (((Boolean) value).booleanValue() ? 1 : 0));
			return new TermQuery(new Term(field, bytesRef.get()));
		}

		throw new SearchIndexException("Boolean field queries require a " + EqualsMatcher.class.getName());
	}
}
