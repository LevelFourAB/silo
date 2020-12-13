package se.l4.silo.engine.internal.tx;

import reactor.core.publisher.Mono;
import se.l4.silo.Transaction;

/**
 * {@link Transaction} that wraps another transaction. This is used internally
 * when the user requests a new transaction when one is already active.
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
	public Mono<Void> rollback()
	{
		// TODO: Should this rollback?
		return Mono.empty();
	}

	@Override
	public Mono<Void> commit()
	{
		return Mono.empty();
	}
}
