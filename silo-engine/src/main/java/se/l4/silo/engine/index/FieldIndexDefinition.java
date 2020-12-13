package se.l4.silo.engine.index;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.IndexDefinition;
import se.l4.silo.engine.internal.index.FieldIndexDefinitionImpl;

public interface FieldIndexDefinition<T>
	extends IndexDefinition<T>
{
	/**
	 * Get the fields that can be queried.
	 *
	 * @return
	 */
	ListIterable<FieldDefinition<T>> getFields();

	/**
	 * Get the fields that can be sorted on.
	 *
	 * @return
	 */
	ListIterable<FieldDefinition<T>> getSortFields();

	/**
	 * Start building a {@link FieldDefinition}.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return
	 */
	public static <T> Builder<T> create(String name, Class<T> type)
	{
		return FieldIndexDefinitionImpl.create(name);
	}

	public interface Builder<T>
	{
		/**
		 * Add a new field that can be queried.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addField(FieldDefinition<T> field);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addSort(FieldDefinition<T> field);

		/**
		 * Create the definition.
		 *
		 * @return
		 */
		FieldIndexDefinition<T> build();
	}


}
