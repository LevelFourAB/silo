package se.l4.silo.engine.search.types;

import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchFields;

/**
 * Field type for indexing binary data.
 * 
 * @author Andreas Holstenson
 *
 */
public final class BinaryFieldType
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

	@Override
	public Query createRangeQuery(String field, Object from, Object to)
	{
		throw new UnsupportedOperationException("binary fields do not support range queries");
	}
}