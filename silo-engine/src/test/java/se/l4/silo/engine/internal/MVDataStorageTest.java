package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import se.l4.aurochs.core.io.Bytes;

/**
 * Tests for {@link MVDataStorage}.
 *  
 * @author Andreas Holstenson
 *
 */
public class MVDataStorageTest
{
	private MVStore store;
	private MVDataStorage storage;
	
	@Before
	public void before()
	{
		store = new MVStore.Builder()
			.fileStore(new OffHeapStore())
			.open();
		storage = new MVDataStorage(store);
	}
	
	@After
	public void after()
	{
		store.close();
	}
	
	private Bytes generateData(int size)
		throws IOException
	{
		return Bytes.viaDataOutput(o -> {
			for(int i=0; i<size; i++)
			{
				o.write(i % 255);
			}
		});
	}
	
	/**
	 * Test that two instances of {@link Bytes} are equal by both checking
	 * their byte arrays and checking their input streams.
	 * 
	 * @param b1
	 * @param b2
	 * @throws IOException
	 */
	private void assertBytesEquals(Bytes b1, Bytes b2)
		throws IOException
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
	
	@Test
	public void testStoreEmptyData()
		throws IOException
	{
		storage.store(1, Bytes.empty());
	}
	
	@Test
	public void testStoreSmallData()
		throws IOException
	{
		storage.store(1, generateData(1024));
	}
	
	@Test
	public void testStoreLargeData()
		throws IOException
	{
		storage.store(1, generateData(1024 * 1024 * 4));
	}
	
	private void testStoreAndRead(Bytes bytes)
		throws IOException
	{
		storage.store(1, bytes);
		Bytes data = storage.get(1);
		
		assertBytesEquals(bytes, data);
	}
	
	@Test
	public void testStoreAndReadEmptyData()
		throws IOException
	{
		testStoreAndRead(Bytes.empty());
	}
	
	@Test
	public void testStoreAndReadSmallData()
		throws IOException
	{
		testStoreAndRead(generateData(1024));
	}
	
	@Test
	public void testStoreAndReadLargeData()
		throws IOException
	{
		testStoreAndRead(generateData(1024 * 1024 * 4));
	}
	
	private void testStoreAndDelete(Bytes data)
		throws IOException
	{
		storage.store(1, data);
		storage.delete(1);
		Assert.assertNull(storage.get(1));
		
		Assert.assertEquals(storage.nextInternalId(), 1l);
	}
	
	@Test
	public void testStoreAndDeleteEmptyData()
		throws IOException
	{
		testStoreAndDelete(Bytes.empty());
	}
	
	@Test
	public void testStoreAndDeleteSmallData()
		throws IOException
	{
		testStoreAndDelete(generateData(1024));
	}
	
	@Test
	public void testStoreAndDeleteLargeData()
		throws IOException
	{
		testStoreAndDelete(generateData(1024 * 1024 * 4));
	}
	
	private void testStoreAndReadMultiple(Bytes b1, Bytes b2)
		throws IOException
	{
		storage.store(1, b1);
		storage.store(2, b2);
		
		assertBytesEquals(storage.get(1), b1);
		assertBytesEquals(storage.get(2), b2);
	}
	
	@Test
	public void testStoreMixedEmptyAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(Bytes.empty(), generateData(1024 * 1024 * 4));
	}
	
	@Test
	public void testStoreMixedLargeAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(generateData(1024 * 1024 * 4), generateData(1024 * 1024 * 4));
	}
	
	@Test
	public void testStoreReadDelete()
		throws IOException
	{
		Bytes b1 = generateData(1024 * 17);
		Bytes b2 = generateData(1024 * 1024 * 4);
		Bytes b3 = generateData(1024 * 1024 * 2);
		storage.store(1, b1);
		storage.store(2, b2);
		storage.store(3, b3);
		
		assertBytesEquals(storage.get(1), b1);
		assertBytesEquals(storage.get(2), b2);
		assertBytesEquals(storage.get(3), b3);
		
		storage.delete(1);
		assertBytesEquals(storage.get(2), b2);
		assertBytesEquals(storage.get(3), b3);
	}
}
