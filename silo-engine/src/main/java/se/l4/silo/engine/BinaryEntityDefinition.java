package se.l4.silo.engine;

import se.l4.silo.Blob;
import se.l4.silo.engine.internal.BinaryEntityDefinitionImpl;

public interface BinaryEntityDefinition<ID>
	extends EntityDefinition<ID, Blob<ID>>
{
/**
	 * Start building a new {@link EntityDefinition}.
	 *
	 * @param <T>
	 * @param name
	 * @param idType
	 * @return
	 */
	public static <ID> Builder<ID> create(String name, Class<ID> idType)
	{
		return BinaryEntityDefinitionImpl.create(name, idType);
	}

	interface Builder<ID>
	{
		/**
		 * Add an index to this entity.
		 *
		 * @param definition
		 *   the index definition
		 * @return
		 */
		Builder<ID> addIndex(IndexDefinition<Blob<ID>> definition);

		/**
		 * Build the definition.
		 *
		 * @return
		 */
		BinaryEntityDefinition<ID> build();
	}
}
