package se.l4.silo.engine.search.types;

import java.io.IOException;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.internal.LocaleAnalyzer;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.search.SearchIndexException;

/**
 * {@link SearchFieldType} for indexing a {@link Locale}.
 */
public class LocaleFieldType
	implements SearchFieldType<Locale>
{
	private static final LocaleAnalyzer ANALYZER = new LocaleAnalyzer();
	private static final FieldType TYPE = new FieldType();

	static
	{
		TYPE.setOmitNorms(true);
		TYPE.setIndexOptions(IndexOptions.DOCS);
		TYPE.setTokenized(true);
		TYPE.freeze();
	}

	@Override
	public Locale read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return Locale.forLanguageTag(in.readString());
	}

	@Override
	public void write(Locale instance, StreamingOutput out)
		throws IOException
	{
		out.writeString(instance.toLanguageTag());
	}

	@Override
	public boolean isLanguageSpecific()
	{
		return false;
	}

	@Override
	public FieldType getDefaultFieldType()
	{
		return TYPE;
	}

	@Override
	public Analyzer getAnalyzer(LocaleSupport lang)
	{
		return ANALYZER;
	}

	@Override
	public IndexableField create(
		String field,
		FieldType type,
		LocaleSupport lang,
		Locale object
	)
	{
		return new Field(field, object.toLanguageTag(), type);
	}

	@Override
	public IndexableField createValuesField(String field, LocaleSupport lang, Locale object)
	{
		return new SortedSetDocValuesField(field, new BytesRef(object.toLanguageTag()));
	}

	@Override
	public IndexableField createSortingField(String field, LocaleSupport lang, Locale object)
	{
		return new SortedDocValuesField(field, new BytesRef(object.toLanguageTag()));
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
			Locale value = (Locale) ((EqualsMatcher) matcher).getValue();
			BooleanQuery.Builder builder = new BooleanQuery.Builder();

			/*
			 * Matching is always done in such a way that all the parts are required. This currently focuses on simplified
			 * matching of a codes with a language and a region.
			 */
			builder.add(new TermQuery(new Term(field, value.toLanguageTag())), Occur.SHOULD);

			if(! value.getCountry().isEmpty() && value.getScript().isEmpty())
			{
				String q = value.getLanguage() + "-*-" + value.getCountry();
				builder.add(new WildcardQuery(new Term(field, q)), Occur.SHOULD);
			}

			return new ConstantScoreQuery(builder.build());
		}

		throw new SearchIndexException("Locale field queries only support EqualsMatcher");
	}
}
