package se.l4.silo.engine.internal;

import se.l4.silo.Transaction;
import se.l4.silo.engine.internal.tx.TransactionExchange;

/**
 * Helper for implementing transaction support.
 * 
 * @author Andreas Holstenson
 *
 */
public interface TransactionSupport
{
	/**
	 * Get the current transaction exchange.
	 * 
	 * @return
	 */
	TransactionExchange getExchange();
	
	/**
	 * Create a new transaction.
	 * 
	 * @return
	 */
	Transaction newTransaction();
}
