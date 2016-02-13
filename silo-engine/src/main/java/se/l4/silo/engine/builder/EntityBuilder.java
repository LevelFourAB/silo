package se.l4.silo.engine.builder;

/**
 * Builder for {@link BinaryEntity} instances. This can be used to register
 * entities at runtime, without having to configure them in manually in a
 * file.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EntityBuilder
{
	/**
	 * Set the name of the backend to use. The backend has to be configured
	 * beforehand or an exception will be raised on entity creation.
	 * 
	 * <p>
	 * The default is {@code default}, which is almost always configured.
	 * 
	 * @param backend
	 * @return
	 */
	EntityBuilder onBackend(String backend);
	
	BinaryBuilder asBinary();
}
