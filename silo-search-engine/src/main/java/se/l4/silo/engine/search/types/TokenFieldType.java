package se.l4.silo.engine.search.types;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.search.SearchIndexException;

public class TokenFieldType
	implements SearchFieldType<String>
{
	private static final Analyzer TOKEN_ANALYZER = new KeywordAnalyzer();

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
	public boolean isLanguageSpecific()
	{
		return false;
	}

	@Override
	public FieldType getDefaultFieldType()
	{
		return StringField.TYPE_NOT_STORED;
	}

	@Override
	public Analyzer getAnalyzer(LocaleSupport lang)
	{
		return TOKEN_ANALYZER;
	}

	@Override
	public IndexableField create(
		String field,
		FieldType type,
		LocaleSupport lang,
		String object
	)
	{
		return new Field(field, object, type);
	}

	@Override
	public IndexableField createValuesField(String field, LocaleSupport lang, String object)
	{
		return new SortedSetDocValuesField(field, new BytesRef(object));
	}

	@Override
	public IndexableField createSortingField(String field, LocaleSupport lang, String object)
	{
		return new SortedDocValuesField(field, new BytesRef(object));
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public Query createQuery(String field, Matcher matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			Object value = ((EqualsMatcher) matcher).getValue();
			return new TermQuery(new Term(field, value.toString()));
		}

		throw new SearchIndexException("Token field queries require a " + EqualsMatcher.class.getName());
	}
}
