package se.l4.silo.index;

import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.RangeMatcher;

/**
 * Builder for limiting a field within a {@link FieldIndexQuery}.
 */
public interface FieldIndexLimitBuilder<ReturnPath, V>
	extends EqualsMatcher.ComposableBuilder<ReturnPath, V>,
		RangeMatcher.ComposableBuilder<ReturnPath, V>
{

}
