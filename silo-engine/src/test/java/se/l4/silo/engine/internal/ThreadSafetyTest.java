package se.l4.silo.engine.internal;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.junit.Test;

import net.jodah.concurrentunit.Waiter;
import se.l4.silo.FetchResult;
import se.l4.silo.Silo;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;
import se.l4.silo.engine.Index;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.structured.ObjectEntity;

public class ThreadSafetyTest
	extends RandomizedTest
{
	@Test
	@ThreadLeakLingering(linger = 10000)
	public void testSeveralThreadsWithBinaryEntity()
		throws Exception
	{
		Path tmp = newTempDir();
		Silo silo = LocalSilo.open(tmp)
			.addEntity("test").asBinary().done()
			.build();

		closeAfterTest(silo);

		ExecutorService executor = Executors.newFixedThreadPool(
			randomIntBetween(4, 8),
			new ThreadFactoryBuilder()
				.setNameFormat("binary-entity-threads-%d")
				.build()
		);
		Waiter waiter = new Waiter();

		BinaryEntity entity = silo.binary("test");

		int entries = scaledRandomIntBetween(1000, 10000) * 2;
		for(int i=0, n=entries; i<n; i++)
		{
			executor.submit(() -> {
				try
				{
					String id = randomUnicodeOfLengthBetween(2, 10);
					int size = randomIntBetween(100, 8192);
					entity.store(id, DataUtils.generate(size));

					executor.submit(() -> {
						try
						{
							// Try to read back the data and verify that it was stored correctly

							FetchResult<BinaryEntry> fr = entity.get(id);
							DataUtils.assertBytesEquals(fr.iterator().next().getData(), DataUtils.generate(size));
						}
						catch(Throwable t)
						{
							waiter.rethrow(t);
						}
						waiter.resume();
					});
				}
				catch(Throwable t)
				{
					waiter.rethrow(t);
				}
				waiter.resume();
			});
		}

		waiter.await(1, TimeUnit.MINUTES, entries);

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.MINUTES);
	}

	@Test
	@ThreadLeakLingering(linger = 10000)
	public void testSeveralThreadsWithObjectEntity()
		throws Exception
	{
		Path tmp = newTempDir();
		Silo silo = LocalSilo.open(tmp)
			.addEntity("test")
				.asStructured()
				.defineField("name", "string")
				.add("byName", Index::queryEngine)
					.addField("name")
					.done()
				.done()
			.build();

		closeAfterTest(silo);

		ExecutorService executor = Executors.newFixedThreadPool(
			randomIntBetween(4, 8),
			new ThreadFactoryBuilder()
				.setNameFormat("object-entity-threads-%d")
				.build()
		);
		Waiter waiter = new Waiter();

		ObjectEntity<TestUserData> entity = silo.structured("test")
			.asObject(TestUserData.class, TestUserData::getId);

		int entries = scaledRandomIntBetween(1000, 10000) * 2;
		for(int i=0, n=entries; i<n; i++)
		{
			executor.submit(() -> {
				try
				{
					int id = randomIntBetween(100, 1000000);
					String name = randomRealisticUnicodeOfLengthBetween(4, 10);
					entity.store(new TestUserData(id, name, 1, false));

					executor.submit(() -> {
						try
						{
							// Try to read back the data and verify that it was stored correctly

							Optional<TestUserData> td = entity.get(id);
							waiter.assertEquals(new TestUserData(id, name, 1, false), td.get());
						}
						catch(Throwable t)
						{
							waiter.rethrow(t);
						}
						waiter.resume();
					});
				}
				catch(Throwable t)
				{
					waiter.rethrow(t);
				}
				waiter.resume();
			});
		}

		waiter.await(1, TimeUnit.MINUTES, entries);

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.MINUTES);
	}
}
