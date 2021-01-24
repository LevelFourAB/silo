package se.l4.silo.engine.index.basic;

import java.util.function.Function;

import se.l4.silo.engine.internal.index.basic.BasicFieldDefinitionImpl;
import se.l4.silo.engine.types.FieldType;

/**
 * Definition of a field that can be queried in an index defined via
 * {@link BasicIndexDefinition}.
 */
public interface BasicFieldDefinition<T, V>
{
	/**
	 * Get the name of the field.
	 */
	String getName();

	/**
	 * Get the type of the field.
	 *
	 * @return
	 */
	FieldType<V> getType();

	/**
	 * Field representing a single value that may be queried.
	 */
	interface Single<T, V>
		extends BasicFieldDefinition<T, V>
	{
		/**
		 * Get a function that can be used to read this field from a certain
		 * object.
		 *
		 * @return
		 */
		Function<T, V> getSupplier();
	}

	/**
	 * Field containing multiple values that may be queried.
	 */
	interface Collection<T, V>
		extends BasicFieldDefinition<T, V>
	{
		/**
		 * Get a function that can be used to read this field from a certain
		 * object.
		 *
		 * @return
		 */
		Function<T, Iterable<V>> getSupplier();
	}

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
		SingleBuilder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Indicate that this field can support multiple values.
		 *
		 * @return
		 */
		CollectionBuilder<T, F> collection();
	}

	interface SingleBuilder<T, F>
	{
		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		SingleBuilder<T, F> withSupplier(Function<T, F> supplier);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		BasicFieldDefinition.Single<T, F> build();
	}

	interface CollectionBuilder<T, F>
	{
		/**
		 * Set the function used to extract the value for the field.
		 *
		 * @param supplier
		 * @return
		 */
		CollectionBuilder<T, F> withSupplier(Function<T, Iterable<F>> supplier);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		BasicFieldDefinition.Collection<T, F> build();
	}
}
