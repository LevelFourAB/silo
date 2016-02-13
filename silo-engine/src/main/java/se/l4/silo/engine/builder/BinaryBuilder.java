package se.l4.silo.engine.builder;

import se.l4.silo.binary.BinaryEntity;

public interface BinaryBuilder
	extends FieldLimited<BinaryBuilder>
{
	/**
	 * Set that this entity is intended to store larger binary data.
	 * 
	 * @return
	 */
	BinaryBuilder setBlob();
	
	/**
	 * Set if this entity is intended to store larger binary data.
	 * 
	 * @param blob
	 * @return
	 */
	BinaryBuilder setBlob(boolean blob);
	
	/**
	 * Add a new index to the builder.
	 * 
	 * @param name
	 * @return
	 */
	EntityIndexBuilder<BinaryBuilder> addIndex(String name);
	
	/**
	 * Build and register the entity.
	 * 
	 * @return
	 */
	BinaryEntity build();
}
