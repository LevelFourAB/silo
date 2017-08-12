package se.l4.silo.engine.search.types;

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

import se.l4.silo.StorageException;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.internal.LocaleAnalyzer;

public class LocaleFieldType
	implements SearchFieldType
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
	public Analyzer getAnalyzer(Language lang)
	{
		return ANALYZER;
	}

	@Override
	public IndexableField create(
			String field,
			FieldType type,
			Language lang,
			Object object)
	{
		String value;
		if(object instanceof Locale)
		{
			value = ((Locale) object).toLanguageTag();
		}
		else if(object instanceof String)
		{
			value = (String) object;
		}
		else if(object == null)
		{
			value = null;
		}
		else
		{
			throw new StorageException("Locale fields can only handle objects of type Locale or String");
		}

		return new Field(field, value, type);
	}

	@Override
	public IndexableField createValuesField(String field, Language lang, Object object)
	{
		String value;
		if(object instanceof Locale)
		{
			value = ((Locale) object).toLanguageTag();
		}
		else if(object instanceof String)
		{
			value = (String) object;
		}
		else
		{
			throw new StorageException("Locale fields can only handle objects of type Locale or String");
		}

		return new SortedSetDocValuesField(field, new BytesRef(value));
	}

	@Override
	public IndexableField createSortingField(String field, Language lang, Object object)
	{
		String value;
		if(object instanceof Locale)
		{
			value = ((Locale) object).toLanguageTag();
		}
		else if(object instanceof String)
		{
			value = (String) object;
		}
		else
		{
			throw new StorageException("Locale fields can only handle objects of type Locale or String");
		}

		return new SortedDocValuesField(field, new BytesRef(value));
	}

	@Override
	public SortField createSortField(String field, boolean ascending, Object params)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public Object extract(IndexableField field)
	{
		return Locale.forLanguageTag(field.stringValue());
	}

	@Override
	public Query createEqualsQuery(String field, Object value)
	{
		Locale locale;
		if(value instanceof String)
		{
			String asString = (String) value;
			if(asString.indexOf('*') >= 0)
			{
				throw new StorageException("Locale querying does not support wildcards");
			}

			locale = Locale.forLanguageTag(asString);
		}
		else if(value instanceof Locale)
		{
			locale = (Locale) value;
		}
		else
		{
			throw new StorageException("Unknown type of query for `" + field + "`: " + value);
		}

		BooleanQuery.Builder builder = new BooleanQuery.Builder();

		/*
		 * Matching is always done in such a way that all the parts are required. This currently focuses on simplified
		 * matching of a codes with a language and a region.
		 */
		builder.add(new TermQuery(new Term(field, locale.toLanguageTag())), Occur.SHOULD);

		if(! locale.getCountry().isEmpty() && locale.getScript().isEmpty())
		{
			String q = locale.getLanguage() + "-*-" + locale.getCountry();
			builder.add(new WildcardQuery(new Term(field, q)), Occur.SHOULD);
		}

		return new ConstantScoreQuery(builder.build());
	}

	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		throw new UnsupportedOperationException("Locale fields do not support range queries; Internal field name was " + field);
	}
}
