package se.l4.silo.engine.index.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import se.l4.silo.index.search.SearchIndexException;

/**
 * Abstract base for field types for numeric values.
 */
public abstract class NumericFieldType<T extends Number>
	implements SearchFieldType<T>
{
	private static final Analyzer ANALYZER = new KeywordAnalyzer();

	private final FieldType type;

	public NumericFieldType()
	{
		FieldType ft = new FieldType();
		ft.setStored(false);
		ft.setTokenized(true);
		ft.setOmitNorms(true);
		ft.setIndexOptions(IndexOptions.DOCS);
		ft.freeze();

		this.type = ft;
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

	public static Number toNumber(Object o)
	{
		return toNumber(o, "Can not convert to number, got: %s");
	}

	public static Number toNumber(Object o, String errorMessage)
	{
		if(o instanceof Number)
		{
			return (Number) o;
		}

		throw new SearchIndexException(String.format(errorMessage, o));
	}
}
