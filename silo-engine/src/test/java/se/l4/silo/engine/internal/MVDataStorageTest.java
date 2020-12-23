package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Transaction;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.internal.tx.WriteableTransactionExchange;

/**
 * Tests for {@link MVDataStorage}.
 */
public class MVDataStorageTest
{
	private MVDataStorage storage;
	private MVStoreManager storeManager;

	@BeforeEach
	public void before()
	{
		storeManager = new MVStoreManagerImpl(new MVStore.Builder()
			.fileStore(new OffHeapStore()));

		storage = new MVDataStorage(
			storeManager,
			new FakeTransactionSupport()
		);
	}

	@AfterEach
	public void after()
		throws IOException
	{
		storeManager.close();
	}

	@Test
	public void testStoreEmptyData()
		throws IOException
	{
		long id = storage.store(out -> {});
		assertThat(id, is(1l));
	}

	@Test
	public void testStoreSmallData()
		throws IOException
	{
		long id = storage.store(DataUtils.generate(1024)::transferTo);
		assertThat(id, is(1l));
	}

	@Test
	public void testStoreLargeData()
		throws IOException
	{
		long id = storage.store(DataUtils.generate(1024 * 1024 * 4)::transferTo);
		assertThat(id, is(1l));
	}

	private void testStoreAndRead(int size)
		throws IOException
	{
		long id = storage.store(DataUtils.generate(size)::transferTo);
		InputStream data = storage.get(null, id);
		DataUtils.assertBytesEquals(DataUtils.generate(size), data);
	}

	@Test
	public void testStoreAndReadEmptyData()
		throws IOException
	{
		testStoreAndRead(0);
	}

	@Test
	public void testStoreAndReadSmallData()
		throws IOException
	{
		testStoreAndRead(1024);
	}

	@Test
	public void testStoreAndReadLargeData()
		throws IOException
	{
		testStoreAndRead(1024 * 1024 * 4);
	}

	private void testStoreAndDelete(int size)
		throws IOException
	{
		long id = storage.store(DataUtils.generate(size)::transferTo);
		storage.delete(id);

		assertThat(storage.get(null, id), nullValue());
		assertThat(storage.nextInternalId(), is(1l));
	}

	@Test
	public void testStoreAndDeleteEmptyData()
		throws IOException
	{
		testStoreAndDelete(0);
	}

	@Test
	public void testStoreAndDeleteSmallData()
		throws IOException
	{
		testStoreAndDelete(1024);
	}

	@Test
	public void testStoreAndDeleteLargeData()
		throws IOException
	{
		testStoreAndDelete(1024 * 1024 * 4);
	}

	private void testStoreAndReadMultiple(int b1, int b2)
		throws IOException
	{
		long id1 = storage.store(DataUtils.generate(b1)::transferTo);
		long id2 = storage.store(DataUtils.generate(b2)::transferTo);

		DataUtils.assertBytesEquals(storage.get(null, id1), DataUtils.generate(b1));
		DataUtils.assertBytesEquals(storage.get(null, id2), DataUtils.generate(b2));
	}

	@Test
	public void testStoreMixedEmptyAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(0, 1024 * 1024 * 4);
	}

	@Test
	public void testStoreMixedLargeAndLarge()
		throws IOException
	{
		testStoreAndReadMultiple(1024 * 1024 * 4, 1024 * 1024 * 4);
	}

	@Test
	public void testStoreReadDelete()
		throws IOException
	{
		long id1 = storage.store(DataUtils.generate(1024 * 17)::transferTo);
		long id2 = storage.store(DataUtils.generate(1024 * 1024 * 4)::transferTo);
		long id3 = storage.store(DataUtils.generate(1024 * 1024 * 2)::transferTo);

		DataUtils.assertBytesEquals(storage.get(null, id1), DataUtils.generate(1024 * 17));
		DataUtils.assertBytesEquals(storage.get(null, id2), DataUtils.generate(1024 * 1024 * 4));
		DataUtils.assertBytesEquals(storage.get(null, id3), DataUtils.generate(1024 * 1024 * 2));

		storage.delete(id1);
		DataUtils.assertBytesEquals(storage.get(null, id2), DataUtils.generate(1024 * 1024 * 4));
		DataUtils.assertBytesEquals(storage.get(null, id3), DataUtils.generate(1024 * 1024 * 2));
	}

	class FakeTransactionSupport
		implements TransactionSupport
	{
		@Override
		public void registerValue(TransactionValue<?> value)
		{
			// Do nothing
		}

		@Override
		public Mono<Void> inTransaction(Runnable runnable)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Mono<T> inTransaction(Supplier<T> supplier)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Mono<Transaction> newTransaction()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Flux<V> transactional(Flux<V> flux)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Mono<V> transactional(Mono<V> mono)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Mono<V> withExchange(Function<WriteableTransactionExchange, V> func)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Mono<V> monoWithExchange(
			Function<WriteableTransactionExchange, Mono<V>> func
		)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Flux<V> fluxWithExchange(
			Function<WriteableTransactionExchange, Flux<V>> func
		)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <V> Flux<V> withTransaction(
			Function<Transaction, Publisher<V>> scopeFunction
		)
		{
			throw new UnsupportedOperationException();
		}
	}
}
