package se.l4.silo.engine.internal.tx;

import java.util.function.Consumer;

import se.l4.commons.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;
import se.l4.silo.Transaction;
import se.l4.silo.engine.internal.log.TransactionLog;

/**
 * {@link Transaction} over a {@link TransactionLog}.
 * 
 * @author Andreas Holstenson
 *
 */
public class TransactionImpl
	implements Transaction, TransactionExchange
{
	private final TransactionLog log;
	private final Consumer<Transaction> onClose;
	private final long id;
	private final TransactionExchange exchange;

	public TransactionImpl(TransactionLog log, Consumer<Transaction> onClose)
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
				return TransactionImpl.this.store(entity, id, bytes);
			}
			
			@Override
			public DeleteResult delete(String entity, Object id)
			{
				return TransactionImpl.this.delete(entity, id);
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
}
