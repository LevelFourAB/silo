package se.l4.silo.engine.search;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;

/**
 * Wrapper for {@link IndexableField}, used to provide custom analyzers and
 * token filters on a per field basis.
 *
 * @author Andreas Holstenson
 *
 */
public class AnalyzerField
	implements IndexableField
{
	private final IndexableField other;
	private final Analyzer analyzer;

	public AnalyzerField(IndexableField other, Analyzer analyzer)
	{
		this.analyzer = analyzer;
		this.other = other;
	}

	@Override
	public String name()
	{
		return other.name();
	}

	@Override
	public IndexableFieldType fieldType()
	{
		return other.fieldType();
	}

	@Override
	public BytesRef binaryValue()
	{
		return other.binaryValue();
	}

	@Override
	public String stringValue()
	{
		return other.stringValue();
	}

	@Override
	public Reader readerValue()
	{
		return other.readerValue();
	}

	@Override
	public Number numericValue()
	{
		return other.numericValue();
	}

	@Override
	public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse)
	{
		return other.tokenStream(this.analyzer, reuse);
	}
}
