package se.l4.silo.engine.internal.tx;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;
import se.l4.silo.Transaction;

/**
 * Exchange to allow entities to interact with a transaction. Exchanges map
 * to a single transaction and create transactions as needed.
 * 
 * @author Andreas Holstenson
 *
 */
public interface TransactionExchange
	extends Transaction
{
	StoreResult store(String entity, Object id, Bytes bytes);
	
	DeleteResult delete(String entity, Object id);
}
