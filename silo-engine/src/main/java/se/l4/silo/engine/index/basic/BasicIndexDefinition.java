package se.l4.silo.engine.index.basic;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.Buildable;
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
	ListIterable<BasicFieldDefinition<T, ?>> getFields();

	/**
	 * Get the fields that can be sorted on.
	 *
	 * @return
	 */
	ListIterable<BasicFieldDefinition.Single<T, ?>> getSortFields();

	/**
	 * Start building a {@link BasicFieldDefinition}.
	 *
	 * @param <T>
	 * @param type
	 * @param name
	 * @return
	 */
	public static <T> Builder<T> create(Class<T> type, String name)
	{
		return BasicIndexDefinitionImpl.create(name);
	}

	public interface Builder<T>
		extends Buildable<BasicIndexDefinition<T>>
	{
		/**
		 * Add a new field that can be queried.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addField(BasicFieldDefinition<T, ?> field);

		/**
		 * Add a new field that can be quired.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<T> addField(Buildable<? extends BasicFieldDefinition<T, ?>> buildable);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addSort(BasicFieldDefinition.Single<T, ?> field);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<T> addSort(Buildable<? extends BasicFieldDefinition.Single<T, ?>> buildable);

		/**
		 * Create the definition.
		 *
		 * @return
		 */
		BasicIndexDefinition<T> build();
	}


}
