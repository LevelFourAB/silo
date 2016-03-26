package se.l4.silo.engine.search;

import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.internal.search.SearchIndexQueryEngine;
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
	public static final SearchFieldType INTEGER = new NumericSearchField(NumericType.INT)
	{
		@Override
		protected IndexableField create(String field, Number number,
				FieldType type)
		{
			return new IntField(field, number.intValue(), type);
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
	};
	
	/**
	 * Field for storing numbers.
	 */
	public static final SearchFieldType LONG = new NumericSearchField(NumericType.LONG)
	{
		@Override
		protected IndexableField create(String field, Number number,
				FieldType type)
		{
			return new LongField(field, number.longValue(), type);
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
