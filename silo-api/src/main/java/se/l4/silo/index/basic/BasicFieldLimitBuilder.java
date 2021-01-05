package se.l4.silo.index.basic;

import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.RangeMatcher;

/**
 * Builder for limiting a field within a {@link BasicIndexQuery}.
 */
public interface BasicFieldLimitBuilder<ReturnPath, V>
	extends EqualsMatcher.ComposableBuilder<ReturnPath, V>,
		RangeMatcher.ComposableBuilder<ReturnPath, V>
{

}
