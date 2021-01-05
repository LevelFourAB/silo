package se.l4.silo.index.search.query;

import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.RangeMatcher;

public interface SearchIndexLimitBuilder<ReturnPath, V>
	extends EqualsMatcher.ComposableBuilder<ReturnPath, V>,
		RangeMatcher.ComposableBuilder<ReturnPath, V>
{

}
