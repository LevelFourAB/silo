package se.l4.silo.index.basic;

import se.l4.silo.internal.BasicFieldRefImpl;

/**
 * A reference to a field in a basic index. Can be used with {@link BasicIndexQuery}
 * to query in a way that is more type safe. The recommended way to use this
 * is to create references once and provide a central location for accessing
 * them.
 *
 * <p>
 * Examples:
 *
 * <pre>
 * public static final BasicFieldRef<String> USERNAME = BasicFieldRef.create("username", String.class);
 *
 * public static final BasicFieldRef<Integer> AGE = BasicFieldRef.create("age", Integer.class);
 * </pre>
 */
public interface BasicFieldRef<V>
{
	/**
	 * Get the name of the field.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Create a reference.
	 *
	 * @param <V>
	 *   the type of the field
	 * @param name
	 *   the name of the field
	 * @param type
	 *   the type of the field
	 * @return
	 *   reference that can be used during querying
	 */
	static <V> BasicFieldRef<V> create(String name, Class<V> type)
	{
		return new BasicFieldRefImpl<>(name, type);
	}
}
