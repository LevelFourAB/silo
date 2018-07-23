package se.l4.silo.engine;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * A snapshot of some data.
 *
 * @author Andreas Holstenson
 *
 */
public interface Snapshot
	extends Closeable
{
	/**
	 * Get this stream as an {@link InputStream}.
	 *
	 * @return
	 * @throws IOException
	 */
	InputStream asStream()
		throws IOException;
}
