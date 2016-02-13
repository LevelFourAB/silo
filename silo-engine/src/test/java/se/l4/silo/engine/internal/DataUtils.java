package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.google.common.base.Throwables;

import se.l4.aurochs.core.io.Bytes;

public class DataUtils
{
	private DataUtils()
	{
	}
	
	public static Bytes generate(int size)
	{
		try
		{
			return Bytes.viaDataOutput(o -> {
				for(int i=0; i<size; i++)
				{
					o.write(i % 255);
				}
			});
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
	}
	
	/**
	 * Test that two instances of {@link Bytes} are equal by both checking
	 * their byte arrays and checking their input streams.
	 * 
	 * @param b1
	 * @param b2
	 * @throws IOException
	 */
	public static void assertBytesEquals(Bytes b1, Bytes b2)
	{
		try
		{
			byte[] a1 = b1.toByteArray();
			byte[] a2 = b2.toByteArray();
			
			if(a1.length != a2.length)
			{
				throw new AssertionError("Bytes not equal, size is different. First is " + a1.length + " bytes, second is " + a2.length);
			}
			
			if(! Arrays.equals(a1, a2))
			{
				throw new AssertionError("Bytes are not equal");
			}
			
			InputStream in1 = b1.asInputStream();
			InputStream in2 = b2.asInputStream();
			
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
			throw Throwables.propagate(e);
		}
	}
}
