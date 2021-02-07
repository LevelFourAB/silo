package se.l4.silo.engine.index.search.internal.facets;

import java.util.Objects;

import se.l4.silo.index.search.facets.FacetValue;

/**
 * Implementation of {@link FacetValue}.
 */
public class FacetValueImpl<V>
	implements FacetValue<V>
{
	private final V item;
	private final int count;

	public FacetValueImpl(
		V item,
		int count
	)
	{
		this.item = item;
		this.count = count;
	}

	@Override
	public V getItem()
	{
		return item;
	}

	@Override
	public int getCount()
	{
		return count;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(count, item);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		FacetValueImpl other = (FacetValueImpl) obj;
		return count == other.count
			&& Objects.equals(item, other.item);
	}

	@Override
	public String toString()
	{
		return "FacetValue{item=" + item + ", count=" + count + "}";
	}
}
