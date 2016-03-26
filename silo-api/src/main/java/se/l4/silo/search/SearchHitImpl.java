package se.l4.silo.search;

import com.google.common.base.MoreObjects;

public class SearchHitImpl<T>
	implements SearchHit<T>
{
	private final T item;

	public SearchHitImpl(T item)
	{
		this.item = item;
	}

	@Override
	public T item()
	{
		return item;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
			.add("item", item)
			.toString();
	}
}
