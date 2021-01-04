package se.l4.silo.engine.search.types;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

/**
 * Version of {@link TextField} that supports a specific {@link Analyzer}.
 */
public class AnalyzingTextField
	extends Field
{
	private final Analyzer analyzer;

	public AnalyzingTextField(
		String name,
		CharSequence value,
		Field.Store store,
		Analyzer analyzer
	)
	{
		super(name, value, store == Store.YES ? TextField.TYPE_STORED : TextField.TYPE_NOT_STORED);

		this.analyzer = analyzer;
	}

	@Override
	public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse)
	{
		return super.tokenStream(this.analyzer, reuse);
	}
}
