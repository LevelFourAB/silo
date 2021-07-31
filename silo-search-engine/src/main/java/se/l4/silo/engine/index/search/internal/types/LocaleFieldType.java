package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;
import java.util.Locale;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexOptions;
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
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.internal.LocaleAnalyzer;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.AnalyzingTextField;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;

/**
 * {@link SearchFieldType} for indexing a {@link Locale}.
 */
public class LocaleFieldType
	implements SearchFieldType<Locale>
{
	private static final LocaleAnalyzer ANALYZER = new LocaleAnalyzer();
	private static final FieldType INDEX_TYPE = new FieldType();

	static
	{
		INDEX_TYPE.setOmitNorms(true);
		INDEX_TYPE.setIndexOptions(IndexOptions.DOCS);
		INDEX_TYPE.setTokenized(true);
		INDEX_TYPE.freeze();
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
	public void create(FieldCreationEncounter<Locale> encounter)
	{
		if(encounter.isIndexed())
		{
			encounter.emit(new AnalyzingTextField(
				encounter.name(),
				encounter.getValue().toLanguageTag(),
				Field.Store.NO,
				ANALYZER
			));
		}

		if(encounter.isSorted())
		{
			encounter.emit(new SortedDocValuesField(
				encounter.sortValuesName(),
				new BytesRef(encounter.getValue().toLanguageTag())
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedSetDocValuesField(
				encounter.docValuesName(),
				new BytesRef(encounter.getValue().toLanguageTag())
			));
		}
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		SearchFieldDef<?> fieldDef,
		Matcher<Locale> matcher
	)
	{
		String fieldName = encounter.index()
			.name(fieldDef, encounter.currentLanguage());

		if(matcher instanceof EqualsMatcher)
		{
			Locale value = ((EqualsMatcher<Locale>) matcher).getValue();
			BooleanQuery.Builder builder = new BooleanQuery.Builder();

			/*
			 * Matching is always done in such a way that all the parts are required. This currently focuses on simplified
			 * matching of a codes with a language and a region.
			 */
			builder.add(new TermQuery(new Term(fieldName, value.toLanguageTag())), Occur.SHOULD);

			if(! value.getCountry().isEmpty() && value.getScript().isEmpty())
			{
				String q = value.getLanguage() + "-*-" + value.getCountry();
				builder.add(new WildcardQuery(new Term(fieldName, q)), Occur.SHOULD);
			}

			return new ConstantScoreQuery(builder.build());
		}

		throw new SearchIndexException("Locale field queries only support EqualsMatcher");
	}
}
