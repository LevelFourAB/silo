package se.l4.silo.engine.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchFields;

public class BooleanFieldType
	implements SearchFieldType
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
		return SearchFields.DEFAULT_ANALYZER;
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

	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		throw new UnsupportedOperationException("Range query unsupported for boolean fields");
	}
}
