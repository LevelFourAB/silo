package se.l4.silo.search.query;

import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.RangeMatcher;

public interface SearchIndexLimitBuilder<ReturnPath, V>
	extends EqualsMatcher.ComposableBuilder<ReturnPath, V>,
		RangeMatcher.ComposableBuilder<ReturnPath, V>
{

}
