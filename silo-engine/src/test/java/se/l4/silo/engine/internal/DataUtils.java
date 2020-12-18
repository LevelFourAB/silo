package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import se.l4.ylem.io.Bytes;

public class DataUtils
{
	private DataUtils()
	{
	}

	public static ByteArrayInputStream generate(int size)
	{
		byte[] out = new byte[size];
		for(int i=0; i<size; i++)
		{
			out[i] = (byte) (i % 255);
		}
		return new ByteArrayInputStream(out);
	}

	/**
	 * Test that two instances of {@link Bytes} are equal by both checking
	 * their byte arrays and checking their input streams.
	 *
	 * @param b1
	 * @param b2
	 * @throws IOException
	 */
	public static void assertBytesEquals(InputStream in1, InputStream in2)
	{
		try
		{
			int i = 0;
			int r1;
			while((r1 = in1.read()) != -1)
			{
				int r2 = in2.read();
				if(r1 != r2)
				{
					throw new AssertionError("Bytes not equal, diverged at index " + i);
				}

				i++;
			}

			if(in2.read() != -1)
			{
				throw new AssertionError("Bytes not equal, second byte stream still has data at index " + i);
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void removeRecursive(Path tmp)
		throws IOException
	{
		Files.walkFileTree(tmp, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException
			{
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException
			{
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
