package se.l4.silo.engine.internal;

import se.l4.silo.Silo;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.QueryEngineFactory;

/**
 * Class for keeping track of different factories registered with a {@link Silo}
 * instance during creation.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EngineFactories
{
	/**
	 * Get the {@link EntityTypeFactory} for the given type.
	 * 
	 * @param type
	 * @return
	 */
	EntityTypeFactory<?, ?> forEntity(String type);

	/**
	 * Get the {@link QueryEngineFactory} for the given type.
	 * 
	 * @param type
	 * @return
	 */
	QueryEngineFactory<?> forQueryEngine(String type);
}
