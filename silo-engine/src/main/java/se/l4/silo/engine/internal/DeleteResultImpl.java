package se.l4.silo.engine.internal;

import se.l4.silo.DeleteResult;

public class DeleteResultImpl<ID, T>
	implements DeleteResult<ID, T>
{
	private final ID id;
	private final boolean wasDeleted;

	public DeleteResultImpl(
		ID id,
		boolean wasDeleted
	)
	{
		this.id = id;
		this.wasDeleted = wasDeleted;
	}

	@Override
	public boolean wasDeleted()
	{
		return wasDeleted;
	}
}
