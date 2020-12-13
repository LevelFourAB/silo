package se.l4.silo.engine.internal.tx;

import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;
import se.l4.silo.Transaction;
import se.l4.silo.engine.internal.log.TransactionLog;
import se.l4.ylem.io.Bytes;

/**
 * {@link Transaction} over a {@link TransactionLog}.
 *
 * @author Andreas Holstenson
 *
 */
public class TransactionExchangeImpl
	implements TransactionExchange
{
	private final TransactionLog log;
	private final Consumer<TransactionExchangeImpl> onClose;
	private final long id;

	private final TransactionExchange exchange;
	private final Transaction transaction;

	public TransactionExchangeImpl(TransactionLog log, Consumer<TransactionExchangeImpl> onClose)
	{
		this.log = log;
		this.onClose = onClose;
		id = log.startTransaction();

		// Create an exchange that does not automatically commit everything
		exchange = new TransactionExchange()
		{
			@Override
			public void rollback()
			{
			}

			@Override
			public void commit()
			{
			}

			@Override
			public StoreResult store(String entity, Object id, Bytes bytes)
			{
				return TransactionExchangeImpl.this.store(entity, id, bytes);
			}

			@Override
			public DeleteResult delete(String entity, Object id)
			{
				return TransactionExchangeImpl.this.delete(entity, id);
			}

			@Override
			public void index(String entity, String index, Object id, Bytes bytes)
			{
				TransactionExchangeImpl.this.index(entity, index, id, bytes);
			}
		};

		transaction = new Transaction()
		{
			@Override
			public Mono<Void> commit()
			{
				return Mono.fromRunnable(TransactionExchangeImpl.this::commit);
			}

			@Override
			public Mono<Void> rollback()
			{
				return Mono.fromRunnable(TransactionExchangeImpl.this::rollback);
			}
		};
	}


	@Override
	public StoreResult store(String entity, Object id, Bytes bytes)
	{
		return log.store(this.id, entity, id, bytes);
	}

	@Override
	public DeleteResult delete(String entity, Object id)
	{
		return log.delete(this.id, entity, id);
	}

	@Override
	public void index(String entity, String index, Object id, Bytes bytes)
	{
		log.storeIndex(this.id, entity, index, id, bytes);
	}

	@Override
	public void rollback()
	{
		log.rollbackTransaction(id);
		onClose.accept(this);
	}

	@Override
	public void commit()
	{
		log.commitTransaction(id);
		onClose.accept(this);
	}

	public TransactionExchange getExchange()
	{
		return exchange;
	}

	public Transaction getTransaction()
	{
		return transaction;
	}
}
