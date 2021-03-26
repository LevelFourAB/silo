package se.l4.silo.engine.internal.tx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.scheduler.Schedulers;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.OpChecker;
import se.l4.silo.engine.internal.StorageApplier;
import se.l4.silo.engine.internal.log.TransactionLogImpl;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.log.DirectApplyLog;
import se.l4.silo.engine.log.Log;
import se.l4.ylem.ids.SimpleLongIdGenerator;

/**
 * Tests for {@link TransactionLogApplier} to check that transactions are
 * translated into a proper set of storage operations.
 *
 */
public class TransactionLogApplierTest
{
	private MVStoreManager store;
	private TransactionLogApplier adapter;
	private OpChecker ops;
	private TransactionLogImpl tx;

	@BeforeEach
	public void before()
	{
		store = new MVStoreManagerImpl(
			Schedulers.newBoundedElastic(1, 100, "test"),
			new MVStore.Builder()
				.fileStore(new OffHeapStore())
		);

		ops = new OpChecker();

		adapter = new TransactionLogApplier(null, null, store, new StorageApplier()
		{
			@Override
			public void transactionStart(long id)
			{
				ops.check("txStart");
			}

			@Override
			public void store(String collection, Object id, InputStream data)
				throws IOException
			{
				ops.check("store", collection, id, data);
			}

			@Override
			public void delete(String collection, Object id)
				throws IOException
			{
				ops.check("delete", collection, id);
			}

			@Override
			public void index(String collection, String index, Object id, InputStream data)
				throws IOException
			{
				ops.check("index", collection, index, id, data);
			}

			@Override
			public void transactionComplete(long id, Throwable t)
			{
				ops.check("txComplete");
			}
		});

		Log log = DirectApplyLog.builder()
			.build(adapter);

		tx = new TransactionLogImpl(log, new SimpleLongIdGenerator());
	}

	@AfterEach
	public void after()
		throws IOException
	{
		store.close();
	}

	private ByteArrayInputStream generateData(int size)
	{
		byte[] out = new byte[size];
		for(int i=0; i<size; i++)
		{
			out[i] = (byte) (i % 255);
		}
		return new ByteArrayInputStream(out);
	}

	private void expectStore(String collection, Object id, InputStream data)
	{
		ops.expect("store", collection, id, data);
	}

	private void expectDelete(String collection, Object id)
	{
		ops.expect("delete", collection, id);
	}

	private void expectIndex(String collection, String index, Object id, InputStream data)
	{
		ops.expect("index", collection, index, id, data);
	}

	private void expectTransactionStart()
	{
		ops.expect("txStart");
	}

	private void expectTransactionComplete()
	{
		ops.expect("txComplete");
	}

	@Test
	public void testEmpty()
	{
		expectTransactionStart();
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testOneEmptyStore()
	{
		expectTransactionStart();
		expectStore("test", 12, new ByteArrayInputStream(new byte[0]));
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.store(id, "test", 12, out -> {});
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testOneSmallStore()
	{
		expectTransactionStart();
		expectStore("test", 12, generateData(1024));
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.store(id, "test", 12, generateData(1024)::transferTo);
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testOneLargeStore()
	{
		expectTransactionStart();
		expectStore("test", 12, generateData(1024 * 1024 * 4));
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.store(id, "test", 12, generateData(1024 * 1024 * 4)::transferTo);
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testIndex()
	{
		expectTransactionStart();
		expectIndex("test", "idx1", 12, generateData(1024));
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.storeIndex(id, "test", "idx1", 12, generateData(1024)::transferTo);
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
		long id = tx.startTransaction();
		tx.store(id, "test", 12, generateData(1024 * 1024 * 4)::transferTo);
		tx.rollbackTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testDelete()
	{
		expectTransactionStart();
		expectDelete("test", 12);
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.delete(id, "test", 12);
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testMultiple()
	{
		expectTransactionStart();
		expectDelete("test", 12);
		expectStore("test", 14, generateData(1024));
		expectTransactionComplete();

		long id = tx.startTransaction();
		tx.delete(id, "test", 12);
		tx.store(id, "test", 14, generateData(1024)::transferTo);
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testLargeTransaction()
	{
		expectTransactionStart();

		for(int i=0; i<2000; i++)
		{
			expectStore("test", i, i % 2 == 0 ? generateData(1024) : generateData(512));
		}

		expectTransactionComplete();

		long id = tx.startTransaction();
		for(int i=0; i<2000; i++)
		{
			tx.store(id, "test", i, (i % 2 == 0 ? generateData(1024) : generateData(512))::transferTo);
		}
		tx.commitTransaction(id);

		ops.checkEmpty();
	}

	@Test
	public void testMultipleCommits()
	{
		expectTransactionStart();
		expectStore("test", 2, generateData(0));
		expectTransactionComplete();

		expectTransactionStart();
		expectStore("test", 1, generateData(0));
		expectTransactionComplete();

		long t1 = tx.startTransaction();
		long t2 = tx.startTransaction();
		tx.store(t1, "test", 1, generateData(0)::transferTo);
		tx.store(t2, "test", 2, generateData(0)::transferTo);
		tx.commitTransaction(t2);
		tx.commitTransaction(t1);

		ops.checkEmpty();
	}

	@Test
	public void testMultipleCommitAndRollback()
	{
		expectTransactionStart();
		expectStore("test", 1, generateData(0));
		expectTransactionComplete();

		long t1 = tx.startTransaction();
		long t2 = tx.startTransaction();
		tx.store(t1, "test", 1, generateData(0)::transferTo);
		tx.store(t2, "test", 2, generateData(0)::transferTo);
		tx.rollbackTransaction(t2);
		tx.commitTransaction(t1);

		ops.checkEmpty();
	}

	@Test
	public void testMultipleCommits2()
	{
		expectTransactionStart();
		expectStore("test", 2, generateData(0));
		expectStore("test", 2, generateData(1024));
		expectTransactionComplete();

		expectTransactionStart();
		expectStore("test", 1, generateData(512));
		expectTransactionComplete();

		long t1 = tx.startTransaction();
		long t2 = tx.startTransaction();
		tx.store(t2, "test", 2, generateData(0)::transferTo);
		tx.store(t1, "test", 1, generateData(512)::transferTo);
		tx.store(t2, "test", 2, generateData(1024)::transferTo);
		tx.commitTransaction(t2);
		tx.commitTransaction(t1);

		ops.checkEmpty();
	}
}
