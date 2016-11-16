package se.l4.silo.search;

import com.google.common.base.MoreObjects;

public class SearchHitImpl<T>
	implements SearchHit<T>
{
	private final T item;
	private final float score;

	public SearchHitImpl(T item, float score)
	{
		this.item = item;
		this.score = score;
	}

	@Override
	public T item()
	{
		return item;
	}
	
	@Override
	public float score()
	{
		return score;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
			.add("item", item)
			.add("score", score)
			.toString();
	}
}
