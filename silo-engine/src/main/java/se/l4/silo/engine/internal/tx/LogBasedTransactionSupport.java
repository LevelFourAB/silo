package se.l4.silo.engine.internal.tx;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import se.l4.silo.StorageException;
import se.l4.silo.Transaction;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.TransactionValueProvider;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.ylem.io.IOConsumer;

/**
 * {@link TransactionSupport} that runs over a {@link TransactionLog}. This
 * class provides reactive and non-reactive support for transactions.
 */
public class LogBasedTransactionSupport
	implements TransactionSupport
{
	private final MVStoreManager storeManager;
	private final TransactionLog log;

	private final Lock acquireLock;

	private final ThreadLocal<ExchangeImpl> activeExchange;
	private final MutableList<TransactionValue<?>> values;

	public LogBasedTransactionSupport(
		MVStoreManager storeManager,
		TransactionLog log,
		Lock acquireLock
	)
	{
		this.storeManager = storeManager;
		this.log = log;
		this.acquireLock = acquireLock;

		activeExchange = new ThreadLocal<>();
		values = Lists.mutable.empty();
	}

	@Override
	public void registerValue(TransactionValue<?> value)
	{
		values.add(value);
	}

	private Context getOrCreateExchange(
		Context context,
		RichIterable<? extends TransactionValue<?>> valuesToCapture
	)
	{
		if(! context.hasKey(ExchangeImpl.class))
		{
			ExchangeImpl exchange = activeExchange.get();
			if(exchange == null)
			{
				acquireLock.lock();
				try
				{
					exchange = new ExchangeImpl(
						log,
						storeManager.acquireVersionHandle(),
						valuesToCapture
					);
				}
				finally
				{
					acquireLock.unlock();
				}
			}

			return context.put(ExchangeImpl.class, exchange);
		}

		return context;
	}

	private Mono<ExchangeImpl> getExchange()
	{
		return Mono.deferContextual(ctx -> {
			ExchangeImpl exchange = ctx.get(ExchangeImpl.class);
			exchange.acquire();
			return Mono.just(exchange);
		});
	}

	private RichIterable<? extends TransactionValue<?>> generateValues(
		TransactionValueProvider... valuesToCapture
	)
	{
		MutableList<TransactionValue<?>> result = Lists.mutable.empty();
		for(TransactionValueProvider p : valuesToCapture)
		{
			p.provideTransactionValues(result::add);
		}
		return result;
	}

	@Override
	public <V> Mono<V> withExchange(
		Function<WriteableTransactionExchange, V> func,
		TransactionValueProvider... valuesToCapture
	)
	{
		return Mono.usingWhen(
			getExchange(),
			e -> Mono.fromSupplier(() -> func.apply(e)),
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, generateValues(valuesToCapture)));
	}

	@Override
	public <V> Mono<V> monoWithExchange(
		Function<WriteableTransactionExchange, Mono<V>> func,
		TransactionValueProvider... valuesToCapture
	)
	{
		return Mono.usingWhen(
			getExchange(),
			func::apply,
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
			).contextWrite(ctx -> getOrCreateExchange(ctx, generateValues(valuesToCapture)));
	}

	@Override
	public <V> Flux<V> fluxWithExchange(
		Function<WriteableTransactionExchange, Flux<V>> func,
		TransactionValueProvider... valuesToCapture
	)
	{
		return Flux.usingWhen(
			getExchange(),
			func::apply,
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
			).contextWrite(ctx -> getOrCreateExchange(ctx, generateValues(valuesToCapture)));
	}

	@Override
	public Mono<Transaction> newTransaction()
	{
		return getExchange()
			.contextWrite(ctx -> getOrCreateExchange(ctx, values))
			.map(TransactionImpl::new);
	}

	@Override
	public <V> Flux<V> transactional(Flux<V> flux)
	{
		return Flux.usingWhen(
			getExchange(),
			e -> flux,
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, values));
	}

	@Override
	public <V> Mono<V> transactional(Mono<V> mono)
	{
		return Mono.usingWhen(
			getExchange(),
			e -> mono,
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, values));
	}

	@Override
	public <V> Flux<V> withTransaction(
		Function<Transaction, Publisher<V>> scopeFunction
	)
	{
		return Flux.usingWhen(
			newTransaction(),
			scopeFunction,
			Transaction::commit,
			(tx, err) -> tx.rollback(),
			Transaction::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, values));
	}

	@Override
	public Mono<Void> inTransaction(Runnable runnable)
	{
		return Mono.<Void, ExchangeImpl>usingWhen(
			getExchange(),
			e -> Mono.fromRunnable(() -> {
				activeExchange.set(e);
				try
				{
					runnable.run();
				}
				finally
				{
					activeExchange.remove();
				}
			}),
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, values));
	}

	@Override
	public <T> Mono<T> inTransaction(Supplier<T> supplier)
	{
		return Mono.usingWhen(
			getExchange(),
			e -> Mono.fromSupplier(() -> {
				activeExchange.set(e);
				try
				{
					return supplier.get();
				}
				finally
				{
					activeExchange.remove();
				}
			}),
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(ctx -> getOrCreateExchange(ctx, values));
	}

	private static class ExchangeImpl
		implements WriteableTransactionExchange
	{
		private final TransactionLog log;
		private final MVStoreManager.VersionHandle versionHandle;
		private final ReentrantLock lock;

		private final MutableMap<TransactionValue<?>, Object> sharedData;

		/** The id of the exchange */
		private long id;

		/** The number of functions currently using this exchange */
		private int handles;

		public ExchangeImpl(
			TransactionLog log,
			MVStoreManager.VersionHandle versionHandle,
			RichIterable<? extends TransactionValue<?>> values
		)
		{
			this.log = log;
			this.versionHandle = versionHandle;
			this.lock = new ReentrantLock();

			long version = versionHandle.getVersion();
			this.sharedData = values.toMap(
				v -> v,
				v -> v.generate(version)
			);
		}

		@Override
		public long getVersion()
		{
			return versionHandle.getVersion();
		}

		@Override
		public void store(String entity, Object id, IOConsumer<OutputStream> generator)
		{
			prepareWrite();

			log.store(this.id, entity, id, generator);
		}

		@Override
		public void delete(String entity, Object id)
		{
			prepareWrite();

			log.delete(this.id, entity, id);
		}

		@Override
		public void index(String entity, String index, Object id, IOConsumer<OutputStream> generator)
		{
			prepareWrite();

			log.storeIndex(this.id, entity, index, id, generator);
		}

		private void prepareWrite()
		{
			lock.lock();
			try
			{
				if(id == 0)
				{
					id = log.startTransaction();
				}
				else if(id == -1)
				{
					throw new StorageException("Transaction has already been committed or rolled back");
				}
			}
			finally
			{
				lock.unlock();
			}
		}

		public void acquire()
		{
			lock.lock();
			try
			{
				handles++;
			}
			finally
			{
				lock.unlock();
			}
		}

		public Mono<Void> rollback()
		{
			return Mono.fromRunnable(() -> {
				lock.lock();
				try
				{
					if(id == -1)
					{
						throw new StorageException("Transaction has already been committed or rolled back");
					}

					if(--handles == 0)
					{
						if(id > 0)
						{
							log.rollbackTransaction(id);
							id = -1;
						}

						sharedData.each(v -> {
							if(v instanceof TransactionValue.Releasable)
							{
								((TransactionValue.Releasable) v).release();
							}
						});

						versionHandle.release();
					}
				}
				finally
				{
					lock.unlock();
				}
			});
		}

		public Mono<Void> commit()
		{
			return Mono.fromRunnable(() -> {
				lock.lock();
				try
				{
					if(id == 0) return;

					if(id == -1)
					{
						throw new StorageException("Transaction has already been committed or rolled back");
					}

					if(--handles == 0)
					{
						if(id > 0)
						{
							log.commitTransaction(id);
							id = -1;
						}

						sharedData.each(v -> {
							if(v instanceof TransactionValue.Releasable)
							{
								((TransactionValue.Releasable) v).release();
							}
						});

						versionHandle.release();
					}
				}
				finally
				{
					lock.unlock();
				}
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(TransactionValue<T> key)
		{
			return (T) sharedData.get(key);
		}

		@Override
		public String toString()
		{
			return "TransactionExchange{id=" + (id == -1 ? "DONE" : (id == 0 ? "READ_ONLY" : id)) + ", handles=" + handles + "}";
		}
	}

	private static class TransactionImpl
		implements Transaction
	{
		private final ExchangeImpl exchange;
		private final AtomicBoolean active;

		public TransactionImpl(ExchangeImpl exchange)
		{
			this.exchange = exchange;
			this.active = new AtomicBoolean(true);
		}

		@Override
		public Mono<Void> commit()
		{
			return Mono.defer(() -> {
				if(active.getAndSet(false))
				{
					return exchange.commit();
				}
				else
				{
					return Mono.empty();
				}
			});
		}

		@Override
		public Mono<Void> rollback()
		{
			return Mono.defer(() -> {
				if(active.getAndSet(false))
				{
					return exchange.rollback();
				}
				else
				{
					return Mono.empty();
				}
			});
		}

		@Override
		public <V> Mono<V> wrap(Mono<V> mono)
		{
			return Mono.usingWhen(
				Mono.just(this),
				tx -> mono,
				tx -> Mono.empty(),
				(tx, err) -> tx.rollback(),
				tx -> Mono.empty()
			)
				.contextWrite(ctx -> ctx.put(ExchangeImpl.class, exchange));
		}

		@Override
		public <V> Flux<V> wrap(Flux<V> flux)
		{
			return Flux.usingWhen(
				Mono.just(this),
				tx -> flux,
				tx -> Mono.empty(),
				(tx, err) -> tx.rollback(),
				tx -> Mono.empty()
			)
				.contextWrite(ctx -> ctx.put(ExchangeImpl.class, exchange));
		}

		@Override
		public <V> Flux<V> execute(
			Function<Transaction, Publisher<V>> scopeFunction
		)
		{
			return Flux.usingWhen(
				Mono.just(this),
				scopeFunction,
				tx -> Mono.empty(),
				(tx, err) -> tx.rollback(),
				tx -> Mono.empty()
			)
				.contextWrite(ctx -> ctx.put(ExchangeImpl.class, exchange));
		}

		@Override
		public String toString()
		{
			return "Transaction{" + (exchange.id == -1 ? "DONE" : (exchange.id == 0 ? "READ_ONLY" : exchange.id)) + "}";
		}
	}
}
