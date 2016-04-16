package se.l4.silo.engine.search;

import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;
import se.l4.silo.search.query.SuggestQuery;

/**
 * Utilities and {@link SearchFieldType types} that can be used within a
 * {@link SearchIndexQueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 */
public class SearchFields
{
	public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();
	private static final Analyzer TOKEN_ANALYZER = new KeywordAnalyzer();
	
	/**
	 * Token field, saved the entire input string as single token. Does not
	 * use localization and does not store the input by default.
	 */
	public static final SearchFieldType TOKEN = new SearchFieldType()
	{
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
		public Analyzer getAnalyzer(Language lang)
		{
			return TOKEN_ANALYZER;
		}
		
		@Override
		public SortField.Type getSortType()
		{
			return SortField.Type.STRING;
		}
		
		@Override
		public IndexableField create(
				String field,
				FieldType type, 
				Language lang,
				Object object)
		{
			return new Field(field, object == null ? null : object.toString(), type);
		}
		
		@Override
		public IndexableField createValuesField(String field, Language lang, Object object)
		{
			return new SortedSetDocValuesField(field, new BytesRef(object.toString()));
		}
		
		@Override
		public IndexableField createSortingField(String field, Language lang, Object object)
		{
			return new SortedDocValuesField(field, new BytesRef(object.toString()));
		}
		
		@Override
		public Object extract(IndexableField field)
		{
			return field.stringValue();
		}
		
		@Override
		public Query createEqualsQuery(String field, Object value)
		{
			return new TermQuery(new Term(field, value.toString()));
		}
	};
	
	/**
	 * Text field.
	 */
	public static final SearchFieldType TEXT = new TextField(false);
	
	/**
	 * Type that can be used for building fields that are suitable for
	 * use with {@link SuggestQuery}. 
	 */
	public static final SearchFieldType SUGGEST = new TextField(true);
	
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType INTEGER = new NumericSearchField()
	{
		@Override
		protected IndexableField create(String field, Number number,
				FieldType type)
		{
			return new IntPoint(field, number.intValue());
		}
		
		@Override
		public IndexableField createValuesField(String field, Language lang, Object object)
		{
			return new NumericDocValuesField(field, ((Number) object).intValue());
		}
		
		@Override
		public SortField.Type getSortType()
		{
			return SortField.Type.INT;
		}
		
		private int toInteger(Object value)
		{
			if(value instanceof Number)
			{
				return ((Number) value).intValue();
			}
			
			return Integer.parseInt(value.toString());
		}
		
		@Override
		public Query createEqualsQuery(String field, Object value)
		{
			return IntPoint.newExactQuery(field, toInteger(value));
		}
		
		@Override
		public IndexableField createSortingField(String field, Language lang, Object object)
		{
			return new NumericDocValuesField(field, toInteger(object));
		}
	};
	
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType LONG = new NumericSearchField()
	{
		@Override
		protected IndexableField create(String field, Number number,
				FieldType type)
		{
			return new LongPoint(field, number.longValue());
		}
		
		@Override
		public IndexableField createValuesField(String field, Language lang, Object object)
		{
			return new NumericDocValuesField(field, ((Number) object).longValue());
		}
		
		@Override
		public SortField.Type getSortType()
		{
			return SortField.Type.LONG;
		}
		
		private long toLong(Object value)
		{
			if(value instanceof Number)
			{
				return ((Number) value).longValue();
			}
			
			return Long.parseLong(value.toString());
		}
		
		@Override
		public Query createEqualsQuery(String field, Object value)
		{
			return LongPoint.newExactQuery(field, toLong(value));
		}
		
		@Override
		public IndexableField createSortingField(String field, Language lang, Object object)
		{
			return new NumericDocValuesField(field, toLong(object));
		}
	};
	
	/**
	 * Type for booleans.
	 */
	public static final SearchFieldType BOOLEAN = new SearchFieldType()
	{
		private final FieldType type = createBooleanFieldType();
		
		@Override
		public boolean isLanguageSpecific()
		{
			return false;
		}
		
		@Override
		public SortField.Type getSortType()
		{
			return SortField.Type.BYTES;
		}
		
		@Override
		public FieldType getDefaultFieldType()
		{
			return type;
		}
		
		@Override
		public Analyzer getAnalyzer(Language lang)
		{
			return DEFAULT_ANALYZER;
		}
		
		@Override
		public Object extract(IndexableField field)
		{
			BytesRef ref = field.binaryValue();
			return ref == null ? null : ref.bytes[ref.offset] == (byte) 1;
		}
		
		@Override
		public IndexableField create(String field, FieldType type, Language lang, Object object)
		{
			byte[] data = object == null ? null : new byte[] { (byte) (((Boolean) object).booleanValue() ? 1 : 0) };
			return new Field(field, data, type);
		}
		
		@Override
		public Query createEqualsQuery(String field, Object value)
		{
			BytesRefBuilder bytesRef = new BytesRefBuilder();
			bytesRef.append((byte) (((Boolean) value).booleanValue() ? 1 : 0));
			return new TermQuery(new Term(field, bytesRef.get()));
		}
	};
	
	protected static FieldType createBooleanFieldType()
	{
		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setIndexOptions(IndexOptions.NONE);
		ft.setTokenized(false);
		ft.freeze();
		return ft;
	}
	
	/**
	 * Token field, saved the entire input string as single token. Does not
	 * use localization and does not store the input by default.
	 */
	public static final SearchFieldType BINARY = new SearchFieldType()
	{
		private final FieldType type = createBinaryFieldType();
		
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
		public Analyzer getAnalyzer(Language lang)
		{
			return DEFAULT_ANALYZER;
		}
		
		@Override
		public SortField.Type getSortType()
		{
			return SortField.Type.BYTES;
		}
		
		@Override
		public IndexableField create(
				String field,
				FieldType type, 
				Language lang,
				Object object)
		{
			return new Field(field, object == null ? null : (byte[]) object, type);
		}
		
		@Override
		public Object extract(IndexableField field)
		{
			BytesRef bytes = field.binaryValue();
			return Arrays.copyOfRange(bytes.bytes, bytes.offset, bytes.offset + bytes.length);
		}
		
		@Override
		public Query createEqualsQuery(String field, Object value)
		{
			return new TermQuery(new Term(field, new BytesRef((byte[]) value)));
		}
	};

	protected static FieldType createBinaryFieldType()
	{
		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setIndexOptions(IndexOptions.NONE);
		ft.setTokenized(false);
		ft.freeze();
		return ft;
	}
	
	private SearchFields()
	{
	}
}
