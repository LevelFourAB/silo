package se.l4.silo.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * {@link Snapshot} implemented over a single file.
 *
 * @author Andreas Holstenson
 *
 */
public class FileSnapshot
	implements Snapshot
{
	private final File file;

	public FileSnapshot(String name)
	{
		this(new File(name));
	}

	public FileSnapshot(Path path)
	{
		this(path.toFile());
	}

	public FileSnapshot(File file)
	{
		this.file = file;
	}

	@Override
	public void close()
		throws IOException
	{
	}

	@Override
	public InputStream asStream()
		throws IOException
	{
		return new FileInputStream(file);
	}

}
