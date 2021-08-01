package se.l4.silo.engine.internal.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;

public class IndexEngineLogTest
{
	private MVStoreManager manager;
	private IndexEngineLog log;

	@BeforeEach
	public void beforeEach()
	{
		manager = new MVStoreManagerImpl(
			null,
			new MVStore.Builder()
				.fileStore(new OffHeapStore())
		);

		log = new IndexEngineLog(manager, "log", id -> {});
	}

	@AfterEach
	public void afterEach()
		throws IOException
	{
		manager.close();
	}

	@Test
	public void testAppendDelete()
	{
		long id = log.appendDelete(1l);
		assertThat(id, is(1l));

		Iterator<LongObjectPair<IndexEngineLog.Entry>> it = log.iterator(0);
		LongObjectPair<IndexEngineLog.Entry> e = it.next();

		assertThat(e.getTwo().getType(), is(IndexEngineLog.EntryType.DELETION));
		assertThat(e.getTwo().getId(), is(1l));
	}

	@Test
	public void testAppendStore()
	{
		long id = log.appendStore(1l, 200l);
		assertThat(id, is(1l));

		Iterator<LongObjectPair<IndexEngineLog.Entry>> it = log.iterator(0);
		LongObjectPair<IndexEngineLog.Entry> e = it.next();

		assertThat(e.getTwo().getType(), is(IndexEngineLog.EntryType.STORE));
		assertThat(e.getTwo().getId(), is(1l));
		assertThat(e.getTwo().getIndexDataId(), is(200l));
	}

	@Test
	public void testSetHardCommitClears()
	{
		log.appendStore(11l, 200l);
		log.appendStore(12l, 200l);
		log.appendStore(13l, 200l);

		assertThat(log.iterator(0).hasNext(), is(true));

		log.setLastHardCommit(3);

		assertThat(log.iterator(0).hasNext(), is(false));

		assertThat(log.getLastHardCommit(), is(3l));
	}

	@Test
	public void testSetHardCommitRemovesOld()
	{
		log.appendStore(1l, 200l);
		log.appendStore(2l, 200l);
		log.appendStore(3l, 200l);

		assertThat(log.iterator(0).hasNext(), is(true));

		log.setLastHardCommit(2);

		Iterator<LongObjectPair<IndexEngineLog.Entry>> it = log.iterator(0);
		LongObjectPair<IndexEngineLog.Entry> e = it.next();

		assertThat(e.getTwo().getType(), is(IndexEngineLog.EntryType.STORE));
		assertThat(e.getTwo().getId(), is(3l));
		assertThat(e.getTwo().getIndexDataId(), is(200l));

		assertThat(log.getLastHardCommit(), is(2l));
	}

	@Test
	public void testRebuildAppend1()
	{
		log.setRebuildMax(1, 200l);
		assertThat(log.getRebuildMax(), is(1l));
		assertThat(log.getRebuildMaxDataId(), is(200l));

		long op1 = log.appendRebuild(2l, 100l);
		assertThat(op1, is(1l));

		assertThat(log.getRebuildMax(), is(0l));
		assertThat(log.getRebuildMaxDataId(), is(0l));

		long op2 = log.appendStore(1l, 200l);
		assertThat(op2, is(2l));
	}

	@Test
	public void testRebuildAppend2()
	{
		log.setRebuildMax(1, 200l);
		assertThat(log.getRebuildMax(), is(1l));
		assertThat(log.getRebuildMaxDataId(), is(200l));

		long op1 = log.appendStore(1l, 200l);
		assertThat(op1, is(2l));

		long op2 = log.appendRebuild(2l, 100l);
		assertThat(op2, is(1l));

		assertThat(log.getRebuildMax(), is(0l));
		assertThat(log.getRebuildMaxDataId(), is(0l));

	}

	@Test
	public void testRebuildAppend3()
	{
		log.setRebuildMax(10, 200l);
		assertThat(log.getRebuildMax(), is(10l));
		assertThat(log.getRebuildMaxDataId(), is(200l));

		long op1 = log.appendRebuild(1l, 100l);
		assertThat(op1, is(1l));

		long op2 = log.appendRebuild(2l, 100l);
		assertThat(op2, is(2l));

		long op3 = log.appendStore(1l, 200l);
		assertThat(op3, is(11l));

		assertThat(log.getRebuildMax(), is(10l));
		assertThat(log.getRebuildMaxDataId(), is(200l));
	}

	@Test
	public void testRebuildPointerEmpty()
	{
		log.setRebuildMax(10, 200l);

		long opId = log.setGenerationPointer(100);
		assertThat(opId, is(1l));

		long dataId = log.getGenerationPointer();
		assertThat(dataId, is(100l));

		opId = log.appendDelete(0l);
		assertThat(opId, is(11l));
	}

	@Test
	public void testRebuildPointerReplace()
	{
		log.setRebuildMax(2, 200l);

		log.setGenerationPointer(100);

		long opId = log.setGenerationPointer(101);
		assertThat(opId, is(2l));

		assertThat(log.size(), is(1));

		long dataId = log.getGenerationPointer();
		assertThat(dataId, is(101l));
	}
}
