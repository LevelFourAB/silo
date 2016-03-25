package se.l4.silo.engine.internal.tx;

import java.io.IOException;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;

import se.l4.commons.id.SimpleLongIdGenerator;
import se.l4.commons.io.Bytes;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.OpChecker;
import se.l4.silo.engine.internal.StorageApplier;
import se.l4.silo.engine.internal.TransactionAdapter;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.log.DirectApplyLog;
import se.l4.silo.engine.log.Log;

/**
 * Tests for {@link TransactionAdapter} to check that transactions are
 * translated into a proper set of storage operations.
 * 
 * @author Andreas Holstenson
 *
 */
public class TransactionAdapterTest
{
	private MVStoreManager store;
	private TransactionAdapter adapter;
	private OpChecker ops;
	private TransactionLogImpl tx;
	
	@Before
	public void before()
	{
		store = new MVStoreManagerImpl(new MVStore.Builder()
			.fileStore(new OffHeapStore()));
		
		ops = new OpChecker();
		
		adapter = new TransactionAdapter(store, new StorageApplier()
		{
			@Override
			public void store(String entity, Object id, Bytes data)
				throws IOException
			{
				ops.check("store", entity, id, data);
			}
			
			@Override
			public void delete(String entity, Object id)
				throws IOException
			{
				ops.check("delete", entity, id);
			}
		});
		
		Log log = DirectApplyLog.builder()
			.build(adapter);
		
		tx = new TransactionLogImpl(log, new SimpleLongIdGenerator());
	}
	
	@After
	public void after()
		throws IOException
	{
		store.close();
	}
	
	private Bytes generateData(int size)
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
	
	private void expectStore(String entity, Object id, Bytes data)
	{
		ops.expect("store", entity, id, data);
	}
	
	private void expectDelete(String entity, Object id)
	{
		ops.expect("delete", entity, id);
	}
	
	@Test
	public void testEmpty()
	{
		long id = tx.startTransaction();
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testOneEmptyStore()
	{
		expectStore("test", 12, Bytes.empty());
		
		long id = tx.startTransaction();
		tx.store(id, "test", 12, Bytes.empty());
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testOneSmallStore()
	{
		Bytes data = generateData(1024);
		expectStore("test", 12, data);
		
		long id = tx.startTransaction();
		tx.store(id, "test", 12, data);
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testOneLargeStore()
	{
		Bytes data = generateData(1024 * 1024 * 4);
		expectStore("test", 12, data);
		
		long id = tx.startTransaction();
		tx.store(id, "test", 12, data);
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testRollbackEmpty()
	{
		long id = tx.startTransaction();
		tx.rollbackTransaction(id);
	}
	
	@Test
	public void testRollbackStore()
	{
		Bytes data = generateData(1024 * 1024 * 4);
		long id = tx.startTransaction();
		tx.store(id, "test", 12, data);
		tx.rollbackTransaction(id);
	}
	
	@Test
	public void testDelete()
	{
		expectDelete("test", 12);
		
		long id = tx.startTransaction();
		tx.delete(id, "test", 12);
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testMultiple()
	{
		Bytes data = generateData(1024);
		expectDelete("test", 12);
		expectStore("test", 14, data);
		
		long id = tx.startTransaction();
		tx.delete(id, "test", 12);
		tx.store(id, "test", 14, data);
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testLargeTransaction()
	{
		Bytes d1 = generateData(1024);
		Bytes d2 = generateData(512);
		for(int i=0; i<2000; i++)
		{
			expectStore("test", i, i % 2 == 0 ? d1 : d2);
		}
		
		long id = tx.startTransaction();
		for(int i=0; i<2000; i++)
		{
			tx.store(id, "test", i, i % 2 == 0 ? d1 : d2);
		}
		tx.commitTransaction(id);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testMultipleCommits()
	{
		expectStore("test", 2, Bytes.empty());
		expectStore("test", 1, Bytes.empty());
		
		long t1 = tx.startTransaction();
		long t2 = tx.startTransaction();
		tx.store(t1, "test", 1, Bytes.empty());
		tx.store(t2, "test", 2, Bytes.empty());
		tx.commitTransaction(t2);
		tx.commitTransaction(t1);
		
		ops.checkEmpty();
	}
	
	@Test
	public void testMultipleCommitAndRollback()
	{
		expectStore("test", 1, Bytes.empty());
		
		long t1 = tx.startTransaction();
		long t2 = tx.startTransaction();
		tx.store(t1, "test", 1, Bytes.empty());
		tx.store(t2, "test", 2, Bytes.empty());
		tx.rollbackTransaction(t2);
		tx.commitTransaction(t1);
		
		ops.checkEmpty();
	}
}
