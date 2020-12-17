package se.l4.silo.engine.internal;

import java.util.Objects;

import se.l4.silo.StoreResult;

public class StoreResultImpl<ID, T>
	implements StoreResult<ID, T>
{
	private final ID id;

	public StoreResultImpl(ID id)
	{
		this.id = id;
	}

	@Override
	public ID getId()
	{
		return id;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		StoreResultImpl other = (StoreResultImpl) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString()
	{
		return "StoreResult{id=" + id + "}";
	}
}
