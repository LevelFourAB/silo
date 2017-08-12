package se.l4.silo.engine.search.internal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.path.PathHierarchyTokenizer;

/**
 * Analyzer for BCP-47 language codes.
 *
 * @author Andreas Holstenson
 *
 */
public class LocaleAnalyzer
	extends Analyzer
{
	@Override
	protected TokenStreamComponents createComponents(String fieldName)
	{
		return new TokenStreamComponents(new PathHierarchyTokenizer('-', '-'));
	}
}
