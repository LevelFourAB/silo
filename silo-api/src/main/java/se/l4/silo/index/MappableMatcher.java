package se.l4.silo.index;

import java.util.function.Function;

/**
 * Marker interface used for {@link Matcher}s that can be mapped.
 */
public interface MappableMatcher<V>
{
	/**
	 * Map this matcher to a new type.
	 *
	 * @param <NV>
	 * @param func
	 * @return
	 */
	<NV> Matcher<NV> map(Function<V, NV> func);
}
