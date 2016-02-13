package se.l4.silo.engine.internal;

import se.l4.silo.binary.BinaryEntity;

/**
 * Listener for changes to the available entities.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EntityChangeListener
{
	/**
	 * {@link BinaryEntity} should be made available.
	 * 
	 * @param name
	 */
	void newBinaryEntity(String name, StorageEntity entity);
	
	/**
	 * Remove an entity.
	 * 
	 * @param name
	 */
	void removeEntity(String name);
}
