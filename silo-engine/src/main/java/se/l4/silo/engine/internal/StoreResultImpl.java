package se.l4.silo.engine.internal;

import java.util.Objects;

import se.l4.silo.StoreResult;

public class StoreResultImpl<ID, T>
	implements StoreResult<ID, T>
{
	private final ID id;
	private final T data;

	public StoreResultImpl(
		ID id,
		T data
	)
	{
		this.id = id;
		this.data =data;
	}

	@Override
	public ID getId()
	{
		return id;
	}

	@Override
	public T getData()
	{
		return data;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(data, id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		StoreResultImpl other = (StoreResultImpl) obj;
		return Objects.equals(data, other.data)
			&& Objects.equals(id, other.id);
	}

	@Override
	public String toString()
	{
		return "StoreResult{id=" + id + ", data=" + data + "}";
	}
}
