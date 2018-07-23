package se.l4.silo.engine.builder;

/**
 * Builder for a field limit.
 *
 * @author Andreas Holstenson
 *
 * @param <R>
 */
public interface FieldDefBuilder<R>
{
	/**
	 * Set the type of the field.
	 *
	 * @param type
	 * @return
	 */
	FieldDefBuilder<R> setType(String type);

	/**
	 * Indicate that this field can have multiple values.
	 *
	 * @return
	 */
	FieldDefBuilder<R> collection();

	/**
	 * Finish this field.
	 *
	 * @return
	 */
	R done();
}
