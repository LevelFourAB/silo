package se.l4.silo.engine.internal.tx;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import se.l4.silo.StorageException;
import se.l4.silo.Transaction;
import se.l4.silo.engine.internal.TransactionSupport;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.ylem.io.IOConsumer;

/**
 * {@link TransactionSupport} that runs over a {@link TransactionLog}. This
 * class provides reactive and non-reactive support for transactions.
 */
public class LogBasedTransactionSupport
	implements TransactionSupport
{
	private final ThreadLocal<ExchangeImpl> activeExchange;
	private final TransactionLog log;

	public LogBasedTransactionSupport(
		TransactionLog log
	)
	{
		this.log = log;

		activeExchange = new ThreadLocal<>();
	}

	private Context getOrCreateExchange(Context context)
	{
		if(! context.hasKey(ExchangeImpl.class))
		{
			ExchangeImpl exchange = activeExchange.get();
			if(exchange == null)
			{
				exchange = new ExchangeImpl(log);
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

	@Override
	public <V> Mono<V> withExchange(Function<TransactionExchange, V> func)
	{
		return Mono.usingWhen(
			getExchange(),
			e -> Mono.fromSupplier(() -> func.apply(e)),
			ExchangeImpl::commit,
			(e, err) -> e.rollback(),
			ExchangeImpl::rollback
		).contextWrite(this::getOrCreateExchange);
	}

	@Override
	public Mono<Transaction> newTransaction()
	{
		return getExchange()
			.contextWrite(this::getOrCreateExchange)
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
		).contextWrite(this::getOrCreateExchange);
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
		).contextWrite(this::getOrCreateExchange);
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
		).contextWrite(this::getOrCreateExchange);
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
		).contextWrite(this::getOrCreateExchange);
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
		).contextWrite(this::getOrCreateExchange);
	}

	private static class ExchangeImpl
		implements TransactionExchange
	{
		private final TransactionLog log;

		private final ReentrantLock lock;

		/** The id of the exchange */
		private long id;

		/** The number of functions currently using this exchange */
		private int handles;

		public ExchangeImpl(TransactionLog log)
		{
			this.log = log;
			this.lock = new ReentrantLock();
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
					if(id == 0) return;

					if(id == -1)
					{
						throw new StorageException("Transaction has already been committed or rolled back");
					}

					if(--handles == 0)
					{
						log.rollbackTransaction(id);
						id = -1;
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
						log.commitTransaction(id);
						id = -1;
					}
				}
				finally
				{
					lock.unlock();
				}
			});
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
