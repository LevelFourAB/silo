package se.l4.silo.engine.search.internal;

import java.util.Objects;

import se.l4.silo.search.SearchHit;

/**
 * Implementation of {@link SearchHit}.
 */
public class SearchHitImpl<T>
	implements SearchHit<T>
{
	private final T item;
	private final float score;

	public SearchHitImpl(
		T item,
		float score
	)
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
	public int hashCode()
	{
		return Objects.hash(item, score);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		SearchHitImpl other = (SearchHitImpl) obj;
		return Objects.equals(item, other.item)
			&& Float.floatToIntBits(score) == Float.floatToIntBits(other.score);
	}

	@Override
	public String toString()
	{
		return "SearchHit{item=" + item + ", score=" + score + "}";
	}
}
