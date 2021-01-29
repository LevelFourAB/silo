package se.l4.silo.engine;

/**
 * Interface used to mark things that can build an object. Implementations
 * of this interface are recommended to be immutable.
 */
@FunctionalInterface
public interface Buildable<T>
{
	/**
	 * Build the object.
	 *
	 * @return
	 *   instance of the object, never {@code null}
	 */
	T build();
}
