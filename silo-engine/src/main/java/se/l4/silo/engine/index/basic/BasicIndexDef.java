package se.l4.silo.engine.index.basic;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.index.IndexDef;
import se.l4.silo.engine.internal.index.basic.BasicIndexDefImpl;

public interface BasicIndexDef<T>
	extends IndexDef<T>
{
	/**
	 * Get the fields that can be queried.
	 *
	 * @return
	 */
	ListIterable<BasicFieldDef<T, ?>> getFields();

	/**
	 * Get the fields that can be sorted on.
	 *
	 * @return
	 */
	ListIterable<BasicFieldDef.Single<T, ?>> getSortFields();

	/**
	 * Start building a {@link BasicFieldDef}.
	 *
	 * @param <T>
	 * @param type
	 * @param name
	 * @return
	 */
	public static <T> Builder<T> create(Class<T> type, String name)
	{
		return BasicIndexDefImpl.create(name);
	}

	public interface Builder<T>
		extends Buildable<BasicIndexDef<T>>
	{
		/**
		 * Add a new field that can be queried.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addField(BasicFieldDef<T, ?> field);

		/**
		 * Add a new field that can be quired.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<T> addField(Buildable<? extends BasicFieldDef<T, ?>> buildable);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param field
		 * @return
		 */
		Builder<T> addSort(BasicFieldDef.Single<T, ?> field);

		/**
		 * Add a field that can be sorted on.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<T> addSort(Buildable<? extends BasicFieldDef.Single<T, ?>> buildable);

		/**
		 * Create the definition.
		 *
		 * @return
		 */
		BasicIndexDef<T> build();
	}


}
