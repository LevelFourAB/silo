package se.l4.silo.engine.internal;

import java.io.IOException;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.engine.MVStoreManager;

/**
 * Tests for {@link MVDataStorage}.
 *  
 * @author Andreas Holstenson
 *
 */
public class MVDataStorageTest
{
	private MVDataStorage storage;
	private MVStoreManager storeManager;
	
	@Before
	public void before()
	{
		MVStore store = new MVStore.Builder()
			.fileStore(new OffHeapStore())
			.open();
		
		storeManager = new MVStoreManagerImpl(store);
		storage = new MVDataStorage(storeManager);
	}
	
	@After
	public void after()
		throws IOException
	{
		storeManager.close();
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
		storage.store(1, DataUtils.generate(1024));
	}
	
	@Test
	public void testStoreLargeData()
		throws IOException
	{
		storage.store(1, DataUtils.generate(1024 * 1024 * 4));
	}
	
	private void testStoreAndRead(Bytes bytes)
		throws IOException
	{
		storage.store(1, bytes);
		Bytes data = storage.get(1);
		
		DataUtils.assertBytesEquals(bytes, data);
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
		testStoreAndRead(DataUtils.generate(1024));
	}
	
	@Test
	public void testStoreAndReadLargeData()
		throws IOException
	{
		testStoreAndRead(DataUtils.generate(1024 * 1024 * 4));
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
		testStoreAndDelete(DataUtils.generate(1024));
	}
	
	@Test
	public void testStoreAndDeleteLargeData()
		throws IOException
	{
		testStoreAndDelete(DataUtils.generate(1024 * 1024 * 4));
	}
	
	private void testStoreAndReadMultiple(Bytes b1, Bytes b2)
		throws IOException
	{
		storage.store(1, b1);
		storage.store(2, b2);
		
		DataUtils.assertBytesEquals(storage.get(1), b1);
		DataUtils.assertBytesEquals(storage.get(2), b2);
	}
	
	@Test
	public void testStoreMixedEmptyAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(Bytes.empty(), DataUtils.generate(1024 * 1024 * 4));
	}
	
	@Test
	public void testStoreMixedLargeAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(DataUtils.generate(1024 * 1024 * 4), DataUtils.generate(1024 * 1024 * 4));
	}
	
	@Test
	public void testStoreReadDelete()
		throws IOException
	{
		Bytes b1 = DataUtils.generate(1024 * 17);
		Bytes b2 = DataUtils.generate(1024 * 1024 * 4);
		Bytes b3 = DataUtils.generate(1024 * 1024 * 2);
		storage.store(1, b1);
		storage.store(2, b2);
		storage.store(3, b3);
		
		DataUtils.assertBytesEquals(storage.get(1), b1);
		DataUtils.assertBytesEquals(storage.get(2), b2);
		DataUtils.assertBytesEquals(storage.get(3), b3);
		
		storage.delete(1);
		DataUtils.assertBytesEquals(storage.get(2), b2);
		DataUtils.assertBytesEquals(storage.get(3), b3);
	}
}
