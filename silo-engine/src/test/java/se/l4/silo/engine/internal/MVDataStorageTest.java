package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
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
import se.l4.silo.engine.internal.mvstore.MVStoreManagerImpl;
import se.l4.silo.engine.internal.tx.TransactionExchange;
import se.l4.ylem.io.Bytes;

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
		long id = storage.store(Bytes.empty());
		assertThat(id, is(1l));
	}

	@Test
	public void testStoreSmallData()
		throws IOException
	{
		long id = storage.store(DataUtils.generate(1024));
		assertThat(id, is(1l));
	}

	@Test
	public void testStoreLargeData()
		throws IOException
	{
		long id = storage.store(DataUtils.generate(1024 * 1024 * 4));
		assertThat(id, is(1l));
	}

	private void testStoreAndRead(Bytes bytes)
		throws IOException
	{
		long id = storage.store(bytes);
		Bytes data = storage.get(null, id);

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
		long id = storage.store(data);
		storage.delete(id);

		assertThat(storage.get(null, id), nullValue());
		assertThat(storage.nextInternalId(), is(1l));
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
		long id1 = storage.store(b1);
		long id2 = storage.store(b2);

		DataUtils.assertBytesEquals(storage.get(null, id1), b1);
		DataUtils.assertBytesEquals(storage.get(null, id2), b2);
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
		long id1 = storage.store(b1);
		long id2 = storage.store(b2);
		long id3 = storage.store(b3);

		DataUtils.assertBytesEquals(storage.get(null, id1), b1);
		DataUtils.assertBytesEquals(storage.get(null, id2), b2);
		DataUtils.assertBytesEquals(storage.get(null, id3), b3);

		storage.delete(id1);
		DataUtils.assertBytesEquals(storage.get(null, id2), b2);
		DataUtils.assertBytesEquals(storage.get(null, id3), b3);
	}

	class FakeTransactionSupport
		implements TransactionSupport
	{
		@Override
		public void registerValue(TransactionalValue<?> value)
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
		public <V> Mono<V> withExchange(Function<TransactionExchange, V> func)
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
