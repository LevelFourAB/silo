package se.l4.silo.engine.internal.query;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Consumer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.DataStorage;
import se.l4.silo.engine.internal.MVDataStorage;
import se.l4.silo.engine.internal.SiloTest;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.query.Query;
import se.l4.ylem.io.Bytes;

public class QueryEngineControllerTest
	extends SiloTest
{
	private MVStoreManager manager;
	private Engine engine;
	private QueryEngineController<String, ?> controller;

	@BeforeEach
	public void beforeEach()
		throws IOException
	{
		recreateEngine();
		reopen();
	}

	@AfterEach
	public void afterEach()
		throws IOException
	{
		manager.close();
	}

	private void recreateEngine()
	{
		engine = new Engine();
	}

	private void reopen()
		throws IOException
	{
		if(manager != null)
		{
			manager.close();
		}

		manager = new MVStoreManagerImpl(
			null,
			new MVStore.Builder()
				.fileName(tmp.resolve("storage.mv.bin").toString())
		);

		DataStorage storage = new MVDataStorage("data", manager);

		controller = new QueryEngineController<>(
			manager,
			storage,
			engine,
			"test::index"
		);
	}

	@Test
	public void testAppendStore()
		throws IOException
	{
		controller.store(1, stream("v1"));

		assertThat(engine.size(), is(1));
		assertThat(engine.get(1), is("v1"));
	}

	@Test
	public void testAppendStore2()
		throws IOException
	{
		controller.store(1, stream("v1"));
		controller.store(2, stream("v2"));

		assertThat(engine.size(), is(2));
		assertThat(engine.get(1), is("v1"));
		assertThat(engine.get(2), is("v2"));
	}

	@Test
	public void testAppendStoreDelete()
		throws IOException
	{
		controller.store(1, stream("v1"));
		controller.delete(1);

		assertThat(engine.size(), is(0));
	}

	@Test
	public void testAppendStoreDeleteStore()
		throws IOException
	{
		controller.store(1, stream("v1"));
		controller.delete(1);
		controller.store(2, stream("v2"));

		assertThat(engine.size(), is(1));
		assertThat(engine.get(2), is("v2"));
	}

	@Test
	public void testStartEmpty()
	{
		Encounter encounter = new Encounter(0);
		controller.start(encounter);

		assertThat(engine.size(), is(0));
	}

	@Test
	public void testDataUpdate()
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter);

		assertThat(engine.size(), is(2));
		assertThat(engine.get(1), is("v1"));
		assertThat(engine.get(2), is("v2"));
	}

	@Test
	public void testDataUpdateWithStore()
		throws IOException
	{
		Encounter encounter = new Encounter(1);
		controller.start(encounter);
		controller.store(2, stream("v2"));

		assertThat(engine.size(), is(2));
		assertThat(engine.get(1), is("v1"));
		assertThat(engine.get(2), is("v2"));
	}

	@Test
	public void testDataUpdateWithDelete()
		throws IOException
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter);
		controller.delete(2);

		assertThat(engine.size(), is(1));
		assertThat(engine.get(1), is("v1"));
	}

	@Test
	public void testReopen()
		throws IOException
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter);

		assertThat(engine.size(), is(2));

		// Mark as committed and reopen
		engine.markCommitted();
		reopen();

		// Start things up again
		controller.start(encounter);

		assertThat(engine.size(), is(2));
		assertThat(engine.lastOpId, is(2l));
	}

	@Test
	public void testRebuildFromZero()
		throws IOException
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter);

		assertThat(engine.size(), is(2));

		// Create and reopen
		recreateEngine();
		reopen();

		// Check that the engine is empty
		assertThat(engine.size(), is(0));

		// Start again
		controller.start(encounter);

		assertThat(engine.size(), is(2));
	}

	// TODO: Disabled until this edge case has a solution
	//@Test
	public void testReopenWithMoreData()
		throws IOException
	{
		controller.start(new Encounter(2));

		assertThat(engine.size(), is(2));

		// Mark as committed and reopen
		engine.markCommitted();
		reopen();

		// Start things up again
		controller.start(new Encounter(3));

		assertThat(engine.size(), is(3));
		assertThat(engine.lastOpId, is(3l));
	}

	private InputStream stream(String value)
	{
		try
		{
			return Bytes.capture(out -> controller.generate(value, out))
				.asInputStream();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static class Engine
		implements QueryEngine<String, FakeQuery>
	{
		private final MutableLongObjectMap<String> data;

		private long lastOpId;
		private long commitOpId;

		public Engine()
		{
			data = LongObjectMaps.mutable.empty();
		}

		@Override
		public void clear()
		{
			commitOpId = 0;
			lastOpId = 0;
			data.clear();
		}

		@Override
		public void generate(String data, ExtendedDataOutputStream out)
			throws IOException
		{
			out.writeString(data);
		}

		@Override
		public void apply(long opId, long dataId, ExtendedDataInputStream in)
			throws IOException
		{
			String value = in.readString();
			data.put(dataId, value);
			lastOpId = opId;
		}

		@Override
		public void delete(long opId, long dataId)
		{
			data.remove(dataId);
			lastOpId = opId;
		}

		@Override
		public long getLastHardCommit()
		{
			return commitOpId;
		}

		public void markCommitted()
		{
			commitOpId = lastOpId;
		}

		public int size()
		{
			return data.size();
		}

		public String get(long dataId)
		{
			return data.get(dataId);
		}

		@Override
		public String getName()
		{
			return "test";
		}

		@Override
		public void provideTransactionValues(
			Consumer<? super TransactionValue<?>> consumer
		)
		{
		}

		@Override
		public void close()
			throws IOException
		{
		}

		@Override
		public Mono<? extends FetchResult<?>> fetch(
			QueryEncounter<? extends FakeQuery, String> encounter
		)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Flux<?> stream(
			QueryEncounter<? extends FakeQuery, String> encounter
		)
		{
			throw new UnsupportedOperationException();
		}
	}

	private static class FakeQuery
		implements Query<String, String, FetchResult<String>>
	{
		@Override
		public String getIndex()
		{
			return null;
		}
	}

	private static class Encounter
		implements QueryEngineRebuildEncounter<String>
	{
		private final long size;

		public Encounter(
			long size
		)
		{
			this.size = size;
		}

		@Override
		public long getSize()
		{
			return size;
		}

		@Override
		public long getLargestId()
		{
			return size;
		}

		@Override
		public Iterator<LongObjectPair<String>> iterator(
			long minIdExclusive,
			long maxIdInclusive
		)
		{
			MutableList<LongObjectPair<String>> result = Lists.mutable.empty();
			for(long i=minIdExclusive+1; i<=maxIdInclusive; i++)
			{
				result.add(PrimitiveTuples.pair(i, "v" + i));
			}
			return result.iterator();
		}
	}
}
