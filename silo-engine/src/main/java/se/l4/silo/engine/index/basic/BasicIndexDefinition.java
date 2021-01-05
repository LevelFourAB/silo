package se.l4.silo.engine.index.basic;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.index.IndexDefinition;
import se.l4.silo.engine.internal.index.basic.BasicIndexDefinitionImpl;

public interface BasicIndexDefinition<T>
	extends IndexDefinition<T>
{
	/**
	 * Get the fields that can be queried.
	 *
	 * @return
	 */
	ListIterable<BasicFieldDefinition<T>> getFields();

	/**
	 * Get the fields that can be sorted on.
	 *
	 * @return
	 */
	ListIterable<BasicFieldDefinition<T>> getSortFields();

	/**
	 * Start building a {@link BasicFieldDefinition}.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return
	 */
	public static <T> Builder<T> create(String name, Class<T> type)
	{
		return BasicIndexDefinitionImpl.create(name);
	}

	public interface Builder<T>
	{
		/**
		 * Add a new field that can be queried.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addField(BasicFieldDefinition<T> field);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addSort(BasicFieldDefinition<T> field);

		/**
		 * Create the definition.
		 *
		 * @return
		 */
		BasicIndexDefinition<T> build();
	}


}
