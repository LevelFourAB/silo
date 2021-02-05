package se.l4.silo.engine;

import java.util.function.Function;

import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.EntityRef;
import se.l4.silo.engine.index.IndexDefinition;
import se.l4.silo.engine.internal.EntityDefinitionImpl;

/**
 * Definition of an {@link se.l4.silo.Entity}.
 */
public interface EntityDefinition<ID, T>
	extends EntityRef<ID, T>
{
	/**
	 * Get the name of the entity.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get the codec used for the entity.
	 */
	EntityCodec<T> getCodec();

	/**
	 * Get a function that can be used to extract the identifier of an object.
	 *
	 * @return
	 */
	Function<T, ID> getIdSupplier();

	/**
	 * Get indexes defined for this entity.
	 *
	 * @return
	 */
	ListIterable<IndexDefinition<T>> getIndexes();

	/**
	 * Start building a new {@link EntityDefinition}.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return
	 */
	public static <T> Builder<Void, T> create(Class<T> type, String name)
	{
		return EntityDefinitionImpl.create(name, type);
	}

	/**
	 * Builder for creating instances of {@link EntityDefinition}.
	 */
	interface Builder<ID, T>
		extends Buildable<EntityDefinition<ID, T>>
	{
		/**
		 * Set the codec that should be used for this entity.
		 *
		 * @param codec
		 * @return
		 */
		Builder<ID, T> withCodec(EntityCodec<T> codec);

		/**
		 * Set how this entity extracts an identifier from the the data.
		 *
		 * @param idFunction
		 * @return
		 */
		<NewID> Builder<NewID, T> withId(Class<NewID> type, Function<T, NewID> idFunction);

		/**
		 * Add an index to this entity.
		 *
		 * @param definition
		 *   the index definition
		 * @return
		 */
		Builder<ID, T> addIndex(IndexDefinition<T> definition);

		/**
		 * Add an index to this entity.
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
		EntityDefinition<ID, T> build();
	}
}
