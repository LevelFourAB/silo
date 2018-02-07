package se.l4.silo.engine.internal.tx;

import se.l4.silo.Transaction;

/**
 * {@link Transaction} that wraps another transaction. This is used internally
 * when the user requests a new transaction when one is already active.
 * 
 * @author Andreas Holstenson
 *
 */
public class WrappedTransaction
	implements Transaction
{
	@SuppressWarnings("unused")
	private final Transaction parent;

	public WrappedTransaction(Transaction parent)
	{
		this.parent = parent;
	}

	@Override
	public void rollback()
	{
		// TODO: Should this rollback?
	}

	@Override
	public void commit()
	{
	}
}
