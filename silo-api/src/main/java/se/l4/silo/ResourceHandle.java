package se.l4.silo;

/**
 * A lock on a resource, used when we know we are going to interact a lot with
 * the storage.
 *
 * @author Andreas Holstenson
 *
 */
public interface ResourceHandle
	extends AutoCloseable
{
	@Override
	void close();
}
