package se.l4.silo.engine.index.search.internal;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;

public class NullFields
{
	public static final FieldType FIELD_TYPE;

	public static final BytesRef VALUE_NULL = new BytesRef(new byte[] { 0 });
	public static final BytesRef VALUE_NON_NULL = new BytesRef(new byte[] { 1 });

	static
	{
		FieldType ft = new FieldType();
		ft.setStored(false);
		ft.setIndexOptions(IndexOptions.DOCS);
		ft.setTokenized(false);
		ft.setOmitNorms(true);
		ft.freeze();

		FIELD_TYPE = ft;
	}

	private NullFields()
	{
	}
}
