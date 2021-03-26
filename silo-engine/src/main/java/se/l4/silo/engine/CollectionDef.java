package se.l4.silo.engine;

import java.util.function.Function;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.CollectionRef;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.silo.engine.internal.CollectionDefImpl;

/**
 * Definition of a {@link se.l4.silo.Collection}.
 */
public interface CollectionDef<ID, T>
	extends CollectionRef<ID, T>
{
	/**
	 * Get the name of the collection.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the codec used for the collection.
	 */
	ObjectCodec<T> getCodec();

	/**
	 * Get a function that can be used to extract the identifier of an object.
	 *
	 * @return
	 */
	Function<T, ID> getIdSupplier();

	/**
	 * Get indexes defined for this collection.
	 *
	 * @return
	 */
	ListIterable<IndexDefinition<T>> getIndexes();

	/**
	 * Start building a new {@link CollectionDef}.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return
	 */
	public static <T> Builder<Void, T> create(Class<T> type, String name)
	{
		return CollectionDefImpl.create(name, type);
	}

	/**
	 * Builder for creating instances of {@link CollectionDef}.
	 */
	interface Builder<ID, T>
		extends Buildable<CollectionDef<ID, T>>
	{
		/**
		 * Set the codec that should be used for this collection.
		 *
		 * @param codec
		 * @return
		 */
		Builder<ID, T> withCodec(ObjectCodec<T> codec);

		/**
		 * Set how this collection extracts an identifier from the the data.
		 *
		 * @param idFunction
		 * @return
		 */
		<NewID> Builder<NewID, T> withId(Class<NewID> type, Function<T, NewID> idFunction);

		/**
		 * Add an index to this collection.
		 *
		 * @param definition
		 *   the index definition
		 * @return
		 */
		Builder<ID, T> addIndex(IndexDefinition<T> definition);

		/**
		 * Add an index to this collection.
		 *
		 * @param buildable
		 * @return
		 */
		Builder<ID, T> addIndex(Buildable<? extends IndexDefinition<T>> buildable);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		CollectionDef<ID, T> build();
	}
}
