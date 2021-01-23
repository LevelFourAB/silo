package se.l4.silo.engine.internal.index;

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

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import se.l4.silo.FetchResult;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexDataGenerator;
import se.l4.silo.engine.index.IndexDataUpdater;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.internal.DataStorage;
import se.l4.silo.engine.internal.MVDataStorage;
import se.l4.silo.engine.internal.SiloTest;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.io.BinaryDataInput;
import se.l4.silo.engine.io.BinaryDataOutput;
import se.l4.silo.index.Query;
import se.l4.ylem.io.Bytes;

public class IndexEngineControllerTest
	extends SiloTest
{
	private MVStoreManager manager;
	private Engine engine;
	private IndexEngineController<String, ?> controller;

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

		controller = new IndexEngineController<>(
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
		controller.start(encounter).block();

		assertThat(engine.size(), is(0));
	}

	@Test
	public void testDataUpdate()
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter).block();

		assertThat(engine.size(), is(2));
		assertThat(engine.get(1), is("v1"));
		assertThat(engine.get(2), is("v2"));
	}

	@Test
	public void testDataUpdateWithStore()
		throws IOException
	{
		Encounter encounter = new Encounter(1);
		controller.start(encounter).block();
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
		controller.start(encounter).block();
		controller.delete(2);

		assertThat(engine.size(), is(1));
		assertThat(engine.get(1), is("v1"));
	}

	@Test
	public void testReopen()
		throws IOException
	{
		Encounter encounter = new Encounter(2);
		Disposable d = controller.start(encounter).block();

		assertThat(engine.size(), is(2));

		// Mark as committed and reopen
		engine.markCommitted();

		d.dispose();
		reopen();

		// Start things up again
		controller.start(encounter).block();

		assertThat(engine.size(), is(2));
		assertThat(engine.lastOpId, is(2l));
	}

	@Test
	public void testRebuildFromZero()
		throws IOException
	{
		Encounter encounter = new Encounter(2);
		controller.start(encounter).block();

		assertThat(engine.size(), is(2));

		// Create and reopen
		recreateEngine();
		reopen();

		// Check that the engine is empty
		assertThat(engine.size(), is(0));

		// Start again
		controller.start(encounter).block();

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
		implements Index<String, FakeQuery>
	{
		private final MutableLongObjectMap<String> data;

		private long lastOpId;
		private long commitOpId;

		private Sinks.Many<Long> hardCommits;

		public Engine()
		{
			data = LongObjectMaps.mutable.empty();

			hardCommits = Sinks.many()
				.multicast()
				.directBestEffort();
		}

		@Override
		public void close()
			throws IOException
		{
		}

		@Override
		public IndexDataGenerator<String> getDataGenerator()
		{
			return (data, out) -> BinaryDataOutput.forStream(out).writeString(data);
		}

		@Override
		public IndexDataUpdater getDataUpdater()
		{
			return new IndexDataUpdater()
			{
				@Override
				public void clear()
				{
					commitOpId = 0;
					lastOpId = 0;
					data.clear();
				}

				@Override
				public void apply(long opId, long dataId, InputStream in)
					throws IOException
				{
					String value = BinaryDataInput.forStream(in).readString();
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

				@Override
				public Flux<Long> hardCommits()
				{
					return hardCommits.asFlux();
				}
			};
		}

		@Override
		public IndexQueryRunner<String, FakeQuery> getQueryRunner()
		{
			return new IndexQueryRunner<String, FakeQuery>()
			{
				@Override
				public void provideTransactionValues(
					Consumer<? super TransactionValue<?>> consumer
				)
				{
				}

				@Override
				public Mono<? extends FetchResult<?>> fetch(
					IndexQueryEncounter<? extends FakeQuery, String> encounter
				)
				{
					throw new UnsupportedOperationException();
				}

				@Override
				public Flux<?> stream(
					IndexQueryEncounter<? extends FakeQuery, String> encounter
				)
				{
					throw new UnsupportedOperationException();
				}
			};
		}

		public void markCommitted()
		{
			commitOpId = lastOpId;
			hardCommits.tryEmitNext(commitOpId);
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
		implements IndexEngineRebuildEncounter<String>
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
