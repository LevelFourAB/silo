package se.l4.silo.engine.index.basic;

import java.util.function.Function;

import se.l4.silo.engine.internal.index.basic.BasicFieldDefinitionImpl;
import se.l4.silo.engine.types.FieldType;

/**
 * Definition of a field that can be queried in an index defined via
 * {@link BasicIndexDefinition}.
 */
public interface BasicFieldDefinition<T>
{
	/**
	 * Get the name of the field.
	 */
	String getName();

	/**
	 * Get a function that can be used to read this field from a certain
	 * object.
	 *
	 * @return
	 */
	Function<T, Object> getSupplier();

	/**
	 * Get if multiple values can exist of this field.
	 *
	 * @return
	 */
	boolean isCollection();

	/**
	 * Get the type of the field.
	 *
	 * @return
	 */
	FieldType<?> getType();

	/**
	 * Start creating a new definition.
	 *
	 * @param <T>
	 * @param name
	 * @return
	 */
	public static <T> Builder<T, Void> create(String name, Class<T> type)
	{
		return BasicFieldDefinitionImpl.create(name);
	}

	interface Builder<T, F>
	{
		/**
		 * Set the type of data this field supports.
		 *
		 * @param type
		 * @return
		 */
		<NF> Builder<T, NF> withType(FieldType<NF> type);

		/**
		 * Set the type of data this field supports by using a known type,
		 * such as primitives or {@link String}.
		 *
		 * @param <NF>
		 * @param type
		 * @return
		 */
		<NF> Builder<T, NF> withType(Class<NF> type);

		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		Builder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Indicate that this field can support multiple values.
		 *
		 * @return
		 */
		Builder<T, Iterable<F>> collection();

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		BasicFieldDefinition<T> build();
	}
}
