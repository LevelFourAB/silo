package se.l4.silo.search;

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

}
