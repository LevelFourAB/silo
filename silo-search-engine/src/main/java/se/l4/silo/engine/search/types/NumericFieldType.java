package se.l4.silo.engine.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.SearchFieldType;

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
	public Analyzer getAnalyzer(LocaleSupport lang)
	{
		return ANALYZER;
	}
}
