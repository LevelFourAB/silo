package se.l4.silo.engine.builder;

import com.google.inject.Provides;

import se.l4.silo.Silo;

/**
 * Builder for instances of {@link Silo}. Should be used in a method annotated
 * with {@link Provides}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface SiloBuilder
{
	/**
	 * Create a new instance via a configuration key.
	 * 
	 * @param configSet
	 * @return
	 */
	Silo viaConfig(String configSet);
}
